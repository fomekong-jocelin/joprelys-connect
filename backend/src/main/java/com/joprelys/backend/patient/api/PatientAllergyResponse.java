package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.PatientAllergyEntity;
import java.time.LocalDate;
import java.util.UUID;

public record PatientAllergyResponse(
        UUID id,
        UUID patientId,
        String substance,
        String severity,
        String reaction,
        String status,
        LocalDate discoveredAt,
        String comment
) {
    public static PatientAllergyResponse fromEntity(PatientAllergyEntity entity) {
        return new PatientAllergyResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getSubstance(),
                entity.getSeverity(),
                entity.getReaction(),
                entity.getStatus(),
                entity.getDiscoveredAt(),
                entity.getComment()
        );
    }
}
