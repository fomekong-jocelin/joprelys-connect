package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.PatientMedicalHistoryEntity;
import java.time.LocalDate;
import java.util.UUID;

public record PatientMedicalHistoryResponse(
        UUID id,
        UUID patientId,
        String category,
        String description,
        LocalDate onsetDate,
        boolean isOngoing,
        String comment
) {
    public static PatientMedicalHistoryResponse fromEntity(PatientMedicalHistoryEntity entity) {
        return new PatientMedicalHistoryResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getOnsetDate(),
                entity.isOngoing(),
                entity.getComment()
        );
    }
}
