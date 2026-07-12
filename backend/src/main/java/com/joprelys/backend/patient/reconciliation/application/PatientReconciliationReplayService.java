package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationCorrectionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionRequest;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationDecisionResponse;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientReconciliationReplayService {

    private final PatientReconciliationEventRepository eventRepository;

    public PatientReconciliationReplayService(PatientReconciliationEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Optional<PatientReconciliationEventEntity> findDecisionReplay(
            UUID sourcePatientId,
            PatientReconciliationDecisionRequest request,
            String idempotencyKey) {
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .map(event -> validateDecisionReplay(event, sourcePatientId, request));
    }

    public Optional<PatientReconciliationEventEntity> findCorrectionReplay(
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request,
            String idempotencyKey) {
        return eventRepository.findByIdempotencyKey(idempotencyKey)
                .map(event -> validateCorrectionReplay(event, sourcePatientId, request));
    }

    public PatientReconciliationDecisionResponse toResponse(
            PatientReconciliationEventEntity event,
            boolean replayed) {
        UUID sourcePatientId = event.getSourcePatient().getId();
        UUID canonicalPatientId = event.getCandidatePatient() == null
                ? sourcePatientId
                : event.getCandidatePatient().getId();
        Set<UUID> contributingPatientIds = new LinkedHashSet<>();
        contributingPatientIds.add(sourcePatientId);
        contributingPatientIds.add(canonicalPatientId);

        return new PatientReconciliationDecisionResponse(
                event.getId(),
                event.getDecision(),
                sourcePatientId,
                event.getResultingIdentityStatus(),
                canonicalPatientId,
                event.getSourcePatient().getTemporaryPatientNumber(),
                contributingPatientIds,
                event.getCreatedAt(),
                replayed);
    }

    private PatientReconciliationEventEntity validateDecisionReplay(
            PatientReconciliationEventEntity event,
            UUID sourcePatientId,
            PatientReconciliationDecisionRequest request) {
        assertSameSource(event, sourcePatientId);
        UUID storedCandidateId = candidateId(event);
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
    }

    private PatientReconciliationEventEntity validateCorrectionReplay(
            PatientReconciliationEventEntity event,
            UUID sourcePatientId,
            PatientReconciliationCorrectionRequest request) {
        assertSameSource(event, sourcePatientId);
        boolean sameRequest = event.getDecision() == PatientReconciliationDecision.CORRECT_LINK
                && Objects.equals(candidateId(event), request.replacementCanonicalPatientId())
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
    }

    private static UUID candidateId(PatientReconciliationEventEntity event) {
        return event.getCandidatePatient() == null ? null : event.getCandidatePatient().getId();
    }

    private static void assertSameSource(PatientReconciliationEventEntity event, UUID sourcePatientId) {
        if (!event.getSourcePatient().getId().equals(sourcePatientId)) {
            throw conflict("IDEMPOTENCY_KEY_REUSED");
        }
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }
}
