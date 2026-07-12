package com.joprelys.backend.patient.reconciliation.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionRequest;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientCanonicalLinkRepository;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PatientReconciliationWorkflowServiceTest {

    private PatientRepository patientRepository;
    private PatientCanonicalLinkRepository canonicalLinkRepository;
    private PatientReconciliationEventRepository eventRepository;
    private PatientReconciliationCandidateService candidateService;
    private PatientReconciliationActorProvider actorProvider;
    private PatientAliasService aliasService;
    private PatientReconciliationRecorder recorder;
    private PatientPairLockService pairLockService;
    private PatientReconciliationQueueService queueService;
    private PatientReconciliationWorkflowService workflowService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        canonicalLinkRepository = mock(PatientCanonicalLinkRepository.class);
        eventRepository = mock(PatientReconciliationEventRepository.class);
        candidateService = mock(PatientReconciliationCandidateService.class);
        actorProvider = mock(PatientReconciliationActorProvider.class);
        aliasService = mock(PatientAliasService.class);
        recorder = mock(PatientReconciliationRecorder.class);
        pairLockService = mock(PatientPairLockService.class);
        queueService = mock(PatientReconciliationQueueService.class);
        PatientReconciliationReplayService replayService =
                new PatientReconciliationReplayService(eventRepository);
        workflowService = new PatientReconciliationWorkflowService(
                patientRepository,
                canonicalLinkRepository,
                eventRepository,
                candidateService,
                actorProvider,
                aliasService,
                recorder,
                pairLockService,
                queueService,
                replayService);
    }

    @Test
    void shouldReplayTheOriginalLinkSnapshotAfterAReplacementCorrection() {
        UUID sourceId = UUID.randomUUID();
        UUID originalTargetId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String idempotencyKey = "replay-original-link";

        PatientEntity source = mock(PatientEntity.class);
        PatientEntity originalTarget = mock(PatientEntity.class);
        PatientReconciliationEventEntity originalEvent = mock(PatientReconciliationEventEntity.class);
        when(source.getId()).thenReturn(sourceId);
        when(source.getTemporaryPatientNumber()).thenReturn("URG-TEMP-20260712-100001");
        when(originalTarget.getId()).thenReturn(originalTargetId);
        when(originalEvent.getId()).thenReturn(eventId);
        when(originalEvent.getSourcePatient()).thenReturn(source);
        when(originalEvent.getCandidatePatient()).thenReturn(originalTarget);
        when(originalEvent.getDecision()).thenReturn(PatientReconciliationDecision.LINK_EXISTING_DPU);
        when(originalEvent.getResultingIdentityStatus()).thenReturn(PatientIdentityStatus.MERGED);
        when(originalEvent.getEvidenceSourceType()).thenReturn(IdentitySourceType.DOCUMENT);
        when(originalEvent.getEvidenceReference()).thenReturn("CNI-001");
        when(originalEvent.getJustification()).thenReturn("Identité confirmée par la pièce originale.");
        when(originalEvent.getCreatedAt()).thenReturn(Instant.parse("2026-07-12T08:00:00Z"));
        when(eventRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(originalEvent));

        PatientReconciliationDecisionRequest request = new PatientReconciliationDecisionRequest(
                PatientReconciliationDecision.LINK_EXISTING_DPU,
                originalTargetId,
                IdentitySourceType.DOCUMENT,
                "CNI-001",
                "Identité confirmée par la pièce originale.");

        var response = workflowService.decide(sourceId, request, idempotencyKey);

        assertEquals(eventId, response.eventId());
        assertEquals(originalTargetId, response.canonicalPatientId());
        assertEquals(PatientIdentityStatus.MERGED, response.sourceIdentityStatus());
        assertEquals(2, response.contributingPatientIds().size());
        assertEquals(true, response.replayed());
        verifyNoInteractions(actorProvider, pairLockService, recorder);
    }

    @Test
    void shouldRejectAContradictoryDecisionAfterCreateNewDpu() {
        UUID organizationId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        String idempotencyKey = "contradictory-decision";

        UserAccountEntity actor = new UserAccountEntity(
                "reviewer@joprelys.local",
                "Reviewer",
                "ADMIN_CLINIQUE",
                "hash");
        actor.setOrganizationId(organizationId);

        PatientEntity source = mock(PatientEntity.class);
        when(source.getId()).thenReturn(sourceId);
        when(source.getOrganizationId()).thenReturn(organizationId);
        when(source.getTemporaryPatientNumber()).thenReturn("URG-TEMP-20260712-100002");
        when(source.getIdentityStatus()).thenReturn(PatientIdentityStatus.VERIFIED);
        when(eventRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(actorProvider.requireCurrentActor()).thenReturn(actor);
        when(patientRepository.findByIdForUpdate(sourceId)).thenReturn(Optional.of(source));
        when(canonicalLinkRepository.findBySourcePatient_Id(sourceId)).thenReturn(Optional.empty());
        when(eventRepository.existsBySourcePatient_IdAndDecision(
                sourceId,
                PatientReconciliationDecision.CREATE_NEW_DPU))
                .thenReturn(true);

        PatientReconciliationDecisionRequest request = new PatientReconciliationDecisionRequest(
                PatientReconciliationDecision.DEFER,
                null,
                IdentitySourceType.DOCUMENT,
                "REVUE-001",
                "Tentative contradictoire après décision finale.");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workflowService.decide(sourceId, request, idempotencyKey));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("PATIENT_RECONCILIATION_FINAL_DECISION_ALREADY_RECORDED", exception.getReason());
        verifyNoInteractions(recorder);
    }
}
