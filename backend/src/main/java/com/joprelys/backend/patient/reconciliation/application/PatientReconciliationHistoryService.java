package com.joprelys.backend.patient.reconciliation.application;

import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.reconciliation.api.PatientReconciliationEventResponse;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventEntity;
import com.joprelys.backend.patient.reconciliation.infrastructure.persistence.PatientReconciliationEventRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PatientReconciliationHistoryService {

    private final PatientRepository patientRepository;
    private final PatientReconciliationEventRepository eventRepository;

    public PatientReconciliationHistoryService(
            PatientRepository patientRepository,
            PatientReconciliationEventRepository eventRepository) {
        this.patientRepository = patientRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientReconciliationEventResponse> listHistory(UUID sourcePatientId) {
        patientRepository.findById(sourcePatientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND"));
        return eventRepository.findAllBySourcePatient_IdOrderByCreatedAtDesc(sourcePatientId).stream()
                .map(this::toResponse)
                .toList();
    }

    private PatientReconciliationEventResponse toResponse(PatientReconciliationEventEntity event) {
        return new PatientReconciliationEventResponse(
                event.getId(),
                event.getDecision(),
                event.getSourcePatient().getId(),
                event.getCandidatePatient() == null ? null : event.getCandidatePatient().getId(),
                event.getPreviousIdentityStatus(),
                event.getResultingIdentityStatus(),
                event.getSimilarityScore(),
                parseReasons(event.getMatchReasons()),
                event.getEvidenceSourceType(),
                event.getEvidenceReference(),
                event.getJustification(),
                event.getCorrectedEventId(),
                event.getCreatedByUserId(),
                event.getCreatedAt());
    }

    private List<String> parseReasons(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(reason -> !reason.isEmpty())
                .toList();
    }
}
