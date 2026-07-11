package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationCandidateResponse;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationCorrectionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionResponse;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationQueueItemResponse;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientReconciliationWorkflowService {

    private static final EnumSet<PatientIdentityStatus> QUEUE_STATUSES = EnumSet.of(
            PatientIdentityStatus.PROVISIONAL_URGENCY,
            PatientIdentityStatus.DECLARED,
            PatientIdentityStatus.VERIFIED);

    private final PatientRepository patientRepository;
    private final PatientCanonicalLinkRepository canonicalLinkRepository;
    private final PatientReconciliationEventRepository eventRepository;
    private final PatientReconciliationCandidateService candidateService;
    private final PatientCanonicalResolver canonicalResolver;
    private final PatientReconciliationActorProvider actorProvider;
    private final PatientAliasService aliasService;
    private final PatientReconciliationRecorder recorder;

    public PatientReconciliationWorkflowService(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository,
            PatientReconciliationEventRepository eventRepository,
            PatientReconciliationCandidateService candidateService,
            PatientCanonicalResolver canonicalResolver,
            PatientReconciliationActorProvider actorProvider,
            PatientAliasService aliasService,
            PatientReconciliationRecorder recorder) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
        this.eventRepository = eventRepository;
        this.candidateService = candidateService;
        this.canonicalResolver = canonicalResolver;
        this.actorProvider = actorProvider;
        this.aliasService = aliasService;
        this.recorder = recorder;
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationQueueItemResponse> listQueue() {
        return patientRepository
                .findAllByTemporaryPatientNumberIsNotNullAndIdentityStatusInOrderByCreatedAtAsc(QUEUE_STATUSES)
                .stream()
                .filter(patient -> !eventRepository.existsBySourcePatient_IdAndDecision(
                        patient.getId(), PatientReconciliationDecision.CREATE_NEW_DPU))
                .map(this::toQueueItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationCandidateResponse> findCandidates(UUID sourcePatientId) {
        return candidateService.findCandidates(sourcePatientId).stream()
                .map(candidate -> new PatientReconciliationCandidateResponse(
                        candidate.patientId(),
                        candidate.globalPatientNumber(),
                        candidate.localPatientNumber(),
                        candidate.displayName(),
                        candidate.gender(),
                        candidate.birthDate(),
                        candidate.phone(),
                        candidate.city(),
                        candidate.score(),
                        candidate.reasons()))
                .toList();
    }

    @Transactional
    public PatientReconciliationDecisionResponse decide(
            UUID sourcePatientId,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Optional<PatientReconciliationEventEntity> replay = findDecisionReplay(
                sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return toDecisionResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        PatientEntity source = lockPatient(sourcePatientId);
        assertSameTenant(actor, source);

        replay = findDecisionReplay(sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return toDecisionResponse(replay.get(), true);
        }
        assertUrgTempSource(source);

        return switch (request.decision()) {
            case CREATE_NEW_DPU -> confirmNewDpu(source, request, normalizedKey, actor);
            case LINK_EXISTING_DPU -> linkExistingDpu(source, request, normalizedKey, actor);
            case DEFER -> deferDecision(source, request, normalizedKey, actor);
            case CORRECT_LINK -> throw conflict("PATIENT_RECONCILIATION_CORRECTION_ENDPOINT_REQUIRED");
        };
    }

    @Transactional
    public PatientReconciliationDecisionResponse correct(
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Optional<PatientReconciliationEventEntity> replay = findCorrectionReplay(
                sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return toDecisionResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        PatientEntity source = lockPatient(sourcePatientId);
        assertSameTenant(actor, source);

        replay = findCorrectionReplay(sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return toDecisionResponse(replay.get(), true);
        }

        PatientCanonicalLinkEntity currentLink = canonicalLinkRepository
                .findForUpdateBySourcePatient_Id(sourcePatientId)
                .orElseThrow(() -> conflict("PATIENT_RECONCILIATION_ACTIVE_LINK_NOT_FOUND"));
        if (!currentLink.getDecisionEvent().getId().equals(request.correctedEventId())) {
            throw conflict("PATIENT_RECONCILIATION_CORRECTED_EVENT_MISMATCH");
        }

        return request.replacementCanonicalPatientId() == null
                ? removeIncorrectLink(source, currentLink, request, normalizedKey, actor)
                : replaceIncorrectLink(source, currentLink, request, normalizedKey, actor);
    }

    private PatientReconciliationDecisionResponse confirmNewDpu(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (source.getIdentityStatus() != PatientIdentityStatus.VERIFIED) {
            throw conflict("PATIENT_RECONCILIATION_VERIFIED_IDENTITY_REQUIRED");
        }
        assertNotAlreadyLinked(source.getId());

        PatientReconciliationEventEntity event = recorder.record(
                actor,
                source,
                null,
                PatientReconciliationDecision.CREATE_NEW_DPU,
                source.getIdentityStatus(),
                source.getIdentityStatus(),
                null,
                null,
                request.evidenceSourceType(),
                request.evidenceReference(),
                request.justification(),
                null,
                idempotencyKey,
                "PATIENT_RECONCILIATION_CREATE_NEW_DPU");

        aliasService.pointOriginAliasesTo(source, source, actor.getId());
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse linkExistingDpu(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (request.candidatePatientId() == null) {
            throw badRequest("PATIENT_RECONCILIATION_CANDIDATE_REQUIRED");
        }
        assertNotAlreadyLinked(source.getId());

        PatientEntity candidate = lockPatient(request.candidatePatientId());
        assertSameTenant(actor, candidate);
        assertEligibleTarget(source, candidate);
        var scoredCandidate = candidateService.scoreCandidate(source.getId(), candidate.getId());

        PatientIdentityStatus previousStatus = source.getIdentityStatus();
        source.transitionIdentityStatus(PatientIdentityStatus.MERGED);
        patientRepository.saveAndFlush(source);

        PatientReconciliationEventEntity event = recorder.record(
                actor,
                source,
                candidate,
                PatientReconciliationDecision.LINK_EXISTING_DPU,
                previousStatus,
                PatientIdentityStatus.MERGED,
                scoredCandidate.score(),
                String.join(",", scoredCandidate.reasons()),
                request.evidenceSourceType(),
                request.evidenceReference(),
                request.justification(),
                null,
                idempotencyKey,
                "PATIENT_RECONCILIATION_LINK_EXISTING_DPU");

        canonicalLinkRepository.save(new PatientCanonicalLinkEntity(
                actor.getOrganizationId(), source, candidate, event, previousStatus));
        aliasService.pointOriginAliasesTo(source, candidate, actor.getId());
        recorder.recordIdentityStatusChange(
                actor, source, previousStatus, PatientIdentityStatus.MERGED, request.justification());
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse deferDecision(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        PatientReconciliationEventEntity event = recorder.record(
                actor,
                source,
                null,
                PatientReconciliationDecision.DEFER,
                source.getIdentityStatus(),
                source.getIdentityStatus(),
                null,
                null,
                request.evidenceSourceType(),
                request.evidenceReference(),
                request.justification(),
                null,
                idempotencyKey,
                "PATIENT_RECONCILIATION_DEFER");
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse removeIncorrectLink(
            PatientEntity source,
            PatientCanonicalLinkEntity currentLink,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        PatientIdentityStatus restoredStatus = currentLink.getSourcePreviousIdentityStatus();
        source.restoreIdentityStatusAfterReconciliationCorrection(restoredStatus);
        patientRepository.saveAndFlush(source);

        PatientReconciliationEventEntity event = recorder.record(
                actor,
                source,
                null,
                PatientReconciliationDecision.CORRECT_LINK,
                PatientIdentityStatus.MERGED,
                restoredStatus,
                null,
                null,
                request.evidenceSourceType(),
                request.evidenceReference(),
                request.justification(),
                currentLink.getDecisionEvent().getId(),
                idempotencyKey,
                "PATIENT_RECONCILIATION_REMOVE_INCORRECT_LINK");

        canonicalLinkRepository.delete(currentLink);
        canonicalLinkRepository.flush();
        aliasService.pointOriginAliasesTo(source, source, actor.getId());
        recorder.recordIdentityStatusChange(
                actor, source, PatientIdentityStatus.MERGED, restoredStatus, request.justification());
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse replaceIncorrectLink(
            PatientEntity source,
            PatientCanonicalLinkEntity currentLink,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        PatientEntity replacement = lockPatient(request.replacementCanonicalPatientId());
        assertSameTenant(actor, replacement);
        assertEligibleTarget(source, replacement);
        if (replacement.getId().equals(currentLink.getCanonicalPatient().getId())) {
            throw conflict("PATIENT_RECONCILIATION_REPLACEMENT_UNCHANGED");
        }

        var scoredCandidate = candidateService.scoreCandidate(source.getId(), replacement.getId());
        PatientReconciliationEventEntity event = recorder.record(
                actor,
                source,
                replacement,
                PatientReconciliationDecision.CORRECT_LINK,
                PatientIdentityStatus.MERGED,
                PatientIdentityStatus.MERGED,
                scoredCandidate.score(),
                String.join(",", scoredCandidate.reasons()),
                request.evidenceSourceType(),
                request.evidenceReference(),
                request.justification(),
                currentLink.getDecisionEvent().getId(),
                idempotencyKey,
                "PATIENT_RECONCILIATION_REPLACE_INCORRECT_LINK");

        PatientIdentityStatus originalStatus = currentLink.getSourcePreviousIdentityStatus();
        canonicalLinkRepository.delete(currentLink);
        canonicalLinkRepository.flush();
        canonicalLinkRepository.save(new PatientCanonicalLinkEntity(
                actor.getOrganizationId(), source, replacement, event, originalStatus));
        aliasService.pointOriginAliasesTo(source, replacement, actor.getId());
        return toDecisionResponse(event, false);
    }

    private Optional<PatientReconciliationEventEntity> findDecisionReplay(
            UUID sourcePatientId,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey) {
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .map(event -> {
                    assertSameSource(event, sourcePatientId);
                    UUID storedCandidateId = event.getCandidatePatient() == null
                            ? null
                            : event.getCandidatePatient().getId();
                    boolean sameRequest = event.getDecision() == request.decision()
                            && Objects.equals(storedCandidateId, request.candidatePatientId())
                            && event.getEvidenceSourceType() == request.evidenceSourceType()
                            && Objects.equals(normalizeNullable(event.getEvidenceReference()),
                                    normalizeNullable(request.evidenceReference()))
                            && Objects.equals(normalizeNullable(event.getJustification()),
                                    normalizeNullable(request.justification()));
                    if (!sameRequest) {
                        throw conflict("IDEMPOTENCY_KEY_REUSED");
                    }
                    return event;
                });
    }

    private Optional<PatientReconciliationEventEntity> findCorrectionReplay(
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey) {
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .map(event -> {
                    assertSameSource(event, sourcePatientId);
                    UUID storedCandidateId = event.getCandidatePatient() == null
                            ? null
                            : event.getCandidatePatient().getId();
                    boolean sameRequest = event.getDecision() == PatientReconciliationDecision.CORRECT_LINK
                            && Objects.equals(storedCandidateId, request.replacementCanonicalPatientId())
                            && Objects.equals(event.getCorrectedEventId(), request.correctedEventId())
                            && event.getEvidenceSourceType() == request.evidenceSourceType()
                            && Objects.equals(normalizeNullable(event.getEvidenceReference()),
                                    normalizeNullable(request.evidenceReference()))
                            && Objects.equals(normalizeNullable(event.getJustification()),
                                    normalizeNullable(request.justification()));
                    if (!sameRequest) {
                        throw conflict("IDEMPOTENCY_KEY_REUSED");
                    }
                    return event;
                });
    }

    private PatientReconciliationQueueItemResponse toQueueItem(PatientEntity patient) {
        UUID canonicalPatientId = canonicalLinkRepository.findBySourcePatient_Id(patient.getId())
                .map(link -> link.getCanonicalPatient().getId())
                .orElse(null);
        return new PatientReconciliationQueueItemResponse(
                patient.getId(),
                patient.getTemporaryPatientNumber(),
                patient.getDisplayName(),
                patient.getIdentityStatus(),
                patient.getIdentityConfidenceLevel(),
                patient.getApparentGender(),
                patient.getEstimatedAgeRange(),
                patient.getFoundAt(),
                patient.getFoundLocation(),
                patient.getCreatedAt(),
                canonicalPatientId);
    }

    private PatientReconciliationDecisionResponse toDecisionResponse(
            PatientReconciliationEventEntity event,
            boolean replayed) {
        var context = canonicalResolver.resolve(event.getSourcePatient().getId());
        return new PatientReconciliationDecisionResponse(
                event.getId(),
                event.getDecision(),
                event.getSourcePatient().getId(),
                event.getSourcePatient().getIdentityStatus(),
                context.canonicalPatient().getId(),
                event.getSourcePatient().getTemporaryPatientNumber(),
                context.contributingPatientIds(),
                event.getCreatedAt(),
                replayed);
    }

    private PatientEntity lockPatient(UUID patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
    }

    private UserAccountEntity requireTenantActor() {
        UserAccountEntity actor = actorProvider.requireCurrentActor();
        if (actor.getOrganizationId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_REQUIRED");
        }
        return actor;
    }

    private void assertNotAlreadyLinked(UUID sourcePatientId) {
        if (canonicalLinkRepository.findBySourcePatient_Id(sourcePatientId).isPresent()) {
            throw conflict("PATIENT_RECONCILIATION_ALREADY_LINKED");
        }
    }

    private static void assertSameTenant(UserAccountEntity actor, PatientEntity patient) {
        if (!actor.getOrganizationId().equals(patient.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND");
        }
    }

    private static void assertUrgTempSource(PatientEntity source) {
        if (source.getTemporaryPatientNumber() == null) {
            throw conflict("PATIENT_RECONCILIATION_SOURCE_NOT_URG_TEMP");
        }
        if (source.getIdentityStatus() == PatientIdentityStatus.MERGED) {
            throw conflict("PATIENT_RECONCILIATION_ALREADY_LINKED");
        }
    }

    private static void assertEligibleTarget(PatientEntity source, PatientEntity candidate) {
        if (source.getId().equals(candidate.getId())) {
            throw conflict("PATIENT_RECONCILIATION_SELF_LINK_FORBIDDEN");
        }
        if (candidate.getIdentityStatus() != PatientIdentityStatus.VERIFIED
                || !"ACTIVE".equals(candidate.getStatus())) {
            throw conflict("PATIENT_RECONCILIATION_TARGET_NOT_ELIGIBLE");
        }
    }

    private static void assertSameSource(PatientReconciliationEventEntity event, UUID sourcePatientId) {
        if (!event.getSourcePatient().getId().equals(sourcePatientId)) {
            throw conflict("IDEMPOTENCY_KEY_REUSED");
        }
    }

    private static String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw badRequest("IDEMPOTENCY_KEY_REQUIRED");
        }
        String normalized = value.trim();
        if (normalized.length() > 120) {
            throw badRequest("IDEMPOTENCY_KEY_TOO_LONG");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }

    private static ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }
}
