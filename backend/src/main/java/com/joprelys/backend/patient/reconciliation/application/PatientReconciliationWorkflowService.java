package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.audit.application.AuditService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientIdentityStatusHistoryRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationCandidateResponse;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationCorrectionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionResponse;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationQueueItemResponse;
import com.joprelys.backend.patient.reconciliation.domain.PatientAliasType;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientIdentityAliasRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
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
    private final PatientIdentityAliasRepository aliasRepository;
    private final PatientReconciliationEventRepository eventRepository;
    private final PatientIdentityStatusHistoryRepository statusHistoryRepository;
    private final PatientReconciliationCandidateService candidateService;
    private final PatientCanonicalResolver canonicalResolver;
    private final PatientReconciliationActorProvider actorProvider;
    private final AuditService auditService;

    public PatientReconciliationWorkflowService(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository,
            PatientIdentityAliasRepository aliasRepository,
            PatientReconciliationEventRepository eventRepository,
            PatientIdentityStatusHistoryRepository statusHistoryRepository,
            PatientReconciliationCandidateService candidateService,
            PatientCanonicalResolver canonicalResolver,
            PatientReconciliationActorProvider actorProvider,
            AuditService auditService) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
        this.aliasRepository = aliasRepository;
        this.eventRepository = eventRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.candidateService = candidateService;
        this.canonicalResolver = canonicalResolver;
        this.actorProvider = actorProvider;
        this.auditService = auditService;
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
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        var replay = eventRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (replay.isPresent()) {
            assertSameSource(replay.get(), sourcePatientId);
            return toDecisionResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        PatientEntity source = lockPatient(sourcePatientId);
        assertSameTenant(actor, source);
        assertUrgTempSource(source);

        if (request.decision() == PatientReconciliationDecision.CORRECT_LINK) {
            throw conflict("PATIENT_RECONCILIATION_CORRECTION_ENDPOINT_REQUIRED");
        }

        return switch (request.decision()) {
            case CREATE_NEW_DPU -> createNewDpuDecision(source, request, normalizedIdempotencyKey, actor);
            case LINK_EXISTING_DPU -> linkExistingDpuDecision(source, request, normalizedIdempotencyKey, actor);
            case DEFER -> deferDecision(source, request, normalizedIdempotencyKey, actor);
            case CORRECT_LINK -> throw conflict("PATIENT_RECONCILIATION_CORRECTION_ENDPOINT_REQUIRED");
        };
    }

    @Transactional
    public PatientReconciliationDecisionResponse correct(
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        var replay = eventRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (replay.isPresent()) {
            assertSameSource(replay.get(), sourcePatientId);
            return toDecisionResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        PatientEntity source = lockPatient(sourcePatientId);
        assertSameTenant(actor, source);

        PatientCanonicalLinkEntity currentLink = canonicalLinkRepository
                .findForUpdateBySourcePatient_Id(sourcePatientId)
                .orElseThrow(() -> conflict("PATIENT_RECONCILIATION_ACTIVE_LINK_NOT_FOUND"));

        if (!currentLink.getDecisionEvent().getId().equals(request.correctedEventId())) {
            throw conflict("PATIENT_RECONCILIATION_CORRECTED_EVENT_MISMATCH");
        }

        if (request.replacementCanonicalPatientId() == null) {
            return removeIncorrectLink(
                    source,
                    currentLink,
                    request,
                    normalizedIdempotencyKey,
                    actor);
        }

        return replaceIncorrectLink(
                source,
                currentLink,
                request,
                normalizedIdempotencyKey,
                actor);
    }

    private PatientReconciliationDecisionResponse createNewDpuDecision(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (source.getIdentityStatus() != PatientIdentityStatus.VERIFIED) {
            throw conflict("PATIENT_RECONCILIATION_VERIFIED_IDENTITY_REQUIRED");
        }
        if (canonicalLinkRepository.findBySourcePatient_Id(source.getId()).isPresent()) {
            throw conflict("PATIENT_RECONCILIATION_ALREADY_LINKED");
        }

        PatientReconciliationEventEntity event = saveEvent(
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
                idempotencyKey);

        pointAliases(source, source, actor.getId());
        audit(actor, source, event, "PATIENT_RECONCILIATION_CREATE_NEW_DPU");
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse linkExistingDpuDecision(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (request.candidatePatientId() == null) {
            throw badRequest("PATIENT_RECONCILIATION_CANDIDATE_REQUIRED");
        }
        if (canonicalLinkRepository.findBySourcePatient_Id(source.getId()).isPresent()) {
            throw conflict("PATIENT_RECONCILIATION_ALREADY_LINKED");
        }

        PatientEntity candidate = lockPatient(request.candidatePatientId());
        assertSameTenant(actor, candidate);
        assertEligibleCanonicalTarget(source, candidate);

        var scoredCandidate = candidateService.scoreCandidate(source.getId(), candidate.getId());
        PatientIdentityStatus previousStatus = source.getIdentityStatus();
        source.transitionIdentityStatus(PatientIdentityStatus.MERGED);
        patientRepository.saveAndFlush(source);

        PatientReconciliationEventEntity event = saveEvent(
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
                idempotencyKey);

        canonicalLinkRepository.save(new PatientCanonicalLinkEntity(
                actor.getOrganizationId(),
                source,
                candidate,
                event,
                previousStatus));
        pointAliases(source, candidate, actor.getId());
        recordStatusChange(source, previousStatus, PatientIdentityStatus.MERGED, request.justification(), actor);
        audit(actor, source, event, "PATIENT_RECONCILIATION_LINK_EXISTING_DPU");
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationDecisionResponse deferDecision(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        PatientReconciliationEventEntity event = saveEvent(
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
                idempotencyKey);

        audit(actor, source, event, "PATIENT_RECONCILIATION_DEFER");
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

        PatientReconciliationEventEntity event = saveEvent(
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
                idempotencyKey);

        canonicalLinkRepository.delete(currentLink);
        canonicalLinkRepository.flush();
        pointAliases(source, source, actor.getId());
        recordStatusChange(source, PatientIdentityStatus.MERGED, restoredStatus, request.justification(), actor);
        audit(actor, source, event, "PATIENT_RECONCILIATION_REMOVE_INCORRECT_LINK");
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
        assertEligibleCanonicalTarget(source, replacement);
        if (replacement.getId().equals(currentLink.getCanonicalPatient().getId())) {
            throw conflict("PATIENT_RECONCILIATION_REPLACEMENT_UNCHANGED");
        }

        var scoredCandidate = candidateService.scoreCandidate(source.getId(), replacement.getId());
        PatientReconciliationEventEntity event = saveEvent(
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
                idempotencyKey);

        PatientIdentityStatus originalStatus = currentLink.getSourcePreviousIdentityStatus();
        canonicalLinkRepository.delete(currentLink);
        canonicalLinkRepository.flush();
        canonicalLinkRepository.save(new PatientCanonicalLinkEntity(
                actor.getOrganizationId(),
                source,
                replacement,
                event,
                originalStatus));
        pointAliases(source, replacement, actor.getId());
        audit(actor, source, event, "PATIENT_RECONCILIATION_REPLACE_INCORRECT_LINK");
        return toDecisionResponse(event, false);
    }

    private PatientReconciliationEventEntity saveEvent(
            UserAccountEntity actor,
            PatientEntity source,
            PatientEntity candidate,
            PatientReconciliationDecision decision,
            PatientIdentityStatus previousStatus,
            PatientIdentityStatus resultingStatus,
            BigDecimal score,
            String matchReasons,
            com.joprelys.backend.patient.domain.IdentitySourceType evidenceSourceType,
            String evidenceReference,
            String justification,
            UUID correctedEventId,
            String idempotencyKey) {
        return eventRepository.saveAndFlush(new PatientReconciliationEventEntity(
                actor.getOrganizationId(),
                source,
                candidate,
                decision,
                previousStatus,
                resultingStatus,
                score,
                matchReasons,
                evidenceSourceType,
                evidenceReference,
                justification,
                correctedEventId,
                idempotencyKey,
                actor.getId()));
    }

    private void pointAliases(PatientEntity source, PatientEntity canonical, UUID actorId) {
        ensureAlias(source, canonical, PatientAliasType.URG_TEMP, source.getTemporaryPatientNumber(), actorId);
        ensureAlias(source, canonical, PatientAliasType.LOCAL_PATIENT_NUMBER, source.getLocalPatientNumber(), actorId);
        ensureAlias(source, canonical, PatientAliasType.GLOBAL_PATIENT_NUMBER, source.getGlobalPatientNumber(), actorId);
    }

    private void ensureAlias(
            PatientEntity source,
            PatientEntity canonical,
            PatientAliasType aliasType,
            String aliasValue,
            UUID actorId) {
        if (aliasValue == null || aliasValue.isBlank()) {
            return;
        }

        var existing = aliasRepository.findByAliasTypeAndAliasValue(aliasType, aliasValue.trim());
        if (existing.isPresent()) {
            if (!existing.get().getOriginPatient().getId().equals(source.getId())) {
                throw conflict("PATIENT_ALIAS_ALREADY_ASSIGNED");
            }
            existing.get().repointTo(canonical);
            aliasRepository.save(existing.get());
            return;
        }

        aliasRepository.save(new PatientIdentityAliasEntity(
                source.getOrganizationId(),
                source,
                canonical,
                aliasType,
                aliasValue,
                actorId));
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

    private static void assertEligibleCanonicalTarget(PatientEntity source, PatientEntity candidate) {
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

    private void recordStatusChange(
            PatientEntity patient,
            PatientIdentityStatus previousStatus,
            PatientIdentityStatus newStatus,
            String reason,
            UserAccountEntity actor) {
        statusHistoryRepository.save(new PatientIdentityStatusHistoryEntity(
                actor.getOrganizationId(),
                patient.getId(),
                previousStatus,
                newStatus,
                reason.trim(),
                actor.getId()));
    }

    private void audit(
            UserAccountEntity actor,
            PatientEntity source,
            PatientReconciliationEventEntity event,
            String action) {
        auditService.logSuccess(
                actor.getId(),
                actor.getOrganizationId(),
                source.getId(),
                "PATIENT_RECONCILIATION",
                event.getId(),
                action,
                "Patient reconciliation decision recorded");
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

    private static ResponseStatusException badRequest(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }

    private static ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }
}
