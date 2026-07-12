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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientReconciliationWorkflowService {

    private final PatientRepository patientRepository;
    private final PatientCanonicalLinkRepository canonicalLinkRepository;
    private final PatientReconciliationEventRepository eventRepository;
    private final PatientReconciliationCandidateService candidateService;
    private final PatientReconciliationActorProvider actorProvider;
    private final PatientAliasService aliasService;
    private final PatientReconciliationRecorder recorder;
    private final PatientPairLockService patientPairLockService;
    private final PatientReconciliationQueueService queueService;
    private final PatientReconciliationReplayService replayService;

    public PatientReconciliationWorkflowService(
            PatientRepository patientRepository,
            PatientCanonicalLinkRepository canonicalLinkRepository,
            PatientReconciliationEventRepository eventRepository,
            PatientReconciliationCandidateService candidateService,
            PatientReconciliationActorProvider actorProvider,
            PatientAliasService aliasService,
            PatientReconciliationRecorder recorder,
            PatientPairLockService patientPairLockService,
            PatientReconciliationQueueService queueService,
            PatientReconciliationReplayService replayService) {
        this.patientRepository = patientRepository;
        this.canonicalLinkRepository = canonicalLinkRepository;
        this.eventRepository = eventRepository;
        this.candidateService = candidateService;
        this.actorProvider = actorProvider;
        this.aliasService = aliasService;
        this.recorder = recorder;
        this.patientPairLockService = patientPairLockService;
        this.queueService = queueService;
        this.replayService = replayService;
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationQueueItemResponse> listQueue() {
        return queueService.listQueue();
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
        Optional<PatientReconciliationEventEntity> replay = replayService.findDecisionReplay(
                sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return replayService.toResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        if (request.decision() == PatientReconciliationDecision.LINK_EXISTING_DPU) {
            return decideLinkExistingDpu(sourcePatientId, request, normalizedKey, actor);
        }

        PatientEntity source = lockPatient(sourcePatientId);
        assertSameTenant(actor, source);
        replay = replayService.findDecisionReplay(sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return replayService.toResponse(replay.get(), true);
        }

        assertUrgTempSource(source);
        assertNoTerminalDecision(source.getId());
        return switch (request.decision()) {
            case CREATE_NEW_DPU -> confirmNewDpu(source, request, normalizedKey, actor);
            case DEFER -> deferDecision(source, request, normalizedKey, actor);
            case LINK_EXISTING_DPU -> throw new IllegalStateException("LINK_DECISION_NOT_ROUTED");
            case CORRECT_LINK -> throw conflict("PATIENT_RECONCILIATION_CORRECTION_ENDPOINT_REQUIRED");
        };
    }

    @Transactional
    public PatientReconciliationDecisionResponse correct(
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Optional<PatientReconciliationEventEntity> replay = replayService.findCorrectionReplay(
                sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return replayService.toResponse(replay.get(), true);
        }

        UserAccountEntity actor = requireTenantActor();
        PatientEntity source;
        PatientEntity replacement = null;
        if (request.replacementCanonicalPatientId() == null) {
            source = lockPatient(sourcePatientId);
        } else {
            if (sourcePatientId.equals(request.replacementCanonicalPatientId())) {
                throw conflict("PATIENT_RECONCILIATION_SELF_LINK_FORBIDDEN");
            }
            var lockedPair = patientPairLockService.lock(
                    sourcePatientId,
                    request.replacementCanonicalPatientId());
            source = lockedPair.left();
            replacement = lockedPair.right();
            assertSameTenant(actor, replacement);
        }
        assertSameTenant(actor, source);

        replay = replayService.findCorrectionReplay(sourcePatientId, request, normalizedKey);
        if (replay.isPresent()) {
            return replayService.toResponse(replay.get(), true);
        }

        PatientCanonicalLinkEntity currentLink = canonicalLinkRepository
                .findForUpdateBySourcePatient_Id(sourcePatientId)
                .orElseThrow(() -> conflict("PATIENT_RECONCILIATION_ACTIVE_LINK_NOT_FOUND"));
        if (!currentLink.getDecisionEvent().getId().equals(request.correctedEventId())) {
            throw conflict("PATIENT_RECONCILIATION_CORRECTED_EVENT_MISMATCH");
        }

        return replacement == null
                ? removeIncorrectLink(source, currentLink, request, normalizedKey, actor)
                : replaceIncorrectLink(source, replacement, currentLink, request, normalizedKey, actor);
    }

    private PatientReconciliationDecisionResponse decideLinkExistingDpu(
            UUID sourcePatientId,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (request.candidatePatientId() == null) {
            throw badRequest("PATIENT_RECONCILIATION_CANDIDATE_REQUIRED");
        }
        if (sourcePatientId.equals(request.candidatePatientId())) {
            throw conflict("PATIENT_RECONCILIATION_SELF_LINK_FORBIDDEN");
        }

        var lockedPair = patientPairLockService.lock(sourcePatientId, request.candidatePatientId());
        PatientEntity source = lockedPair.left();
        PatientEntity candidate = lockedPair.right();
        assertSameTenant(actor, source);
        assertSameTenant(actor, candidate);

        Optional<PatientReconciliationEventEntity> replay = replayService.findDecisionReplay(
                sourcePatientId, request, idempotencyKey);
        if (replay.isPresent()) {
            return replayService.toResponse(replay.get(), true);
        }

        assertUrgTempSource(source);
        assertNoTerminalDecision(source.getId());
        return linkExistingDpu(source, candidate, request, idempotencyKey, actor);
    }

    private PatientReconciliationDecisionResponse confirmNewDpu(
            PatientEntity source,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        if (source.getIdentityStatus() != PatientIdentityStatus.VERIFIED) {
            throw conflict("PATIENT_RECONCILIATION_VERIFIED_IDENTITY_REQUIRED");
        }

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
        return replayService.toResponse(event, false);
    }

    private PatientReconciliationDecisionResponse linkExistingDpu(
            PatientEntity source,
            PatientEntity candidate,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        candidateService.assertEligibleTarget(source, candidate);
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
        return replayService.toResponse(event, false);
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
        return replayService.toResponse(event, false);
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
        return replayService.toResponse(event, false);
    }

    private PatientReconciliationDecisionResponse replaceIncorrectLink(
            PatientEntity source,
            PatientEntity replacement,
            PatientCanonicalLinkEntity currentLink,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey,
            UserAccountEntity actor) {
        candidateService.assertEligibleTarget(source, replacement);
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
        return replayService.toResponse(event, false);
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

    private void assertNoTerminalDecision(UUID sourcePatientId) {
        boolean linked = canonicalLinkRepository.findBySourcePatient_Id(sourcePatientId).isPresent();
        boolean confirmedAsNewDpu = eventRepository.existsBySourcePatient_IdAndDecision(
                sourcePatientId,
                PatientReconciliationDecision.CREATE_NEW_DPU);
        if (linked || confirmedAsNewDpu) {
            throw conflict("PATIENT_RECONCILIATION_FINAL_DECISION_ALREADY_RECORDED");
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
