package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.infrastructure.persistence.PatientVaccinationEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientVaccinationResponse(
        UUID id,
        UUID patientId,
        String vaccineName,
        String batchNumber,
        LocalDate administeredAt,
        String administeredBy,
        String notes,
        LocalDate nextDoseAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PatientVaccinationResponse fromEntity(PatientVaccinationEntity entity) {
        if (entity == null) return null;
        return new PatientVaccinationResponse(
                entity.getId(),
                entity.getPatientId(),
                entity.getVaccineName(),
                entity.getBatchNumber(),
                entity.getAdministeredAt(),
                entity.getAdministeredBy(),
                entity.getNotes(),
                entity.getNextDoseAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
