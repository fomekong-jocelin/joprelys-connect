package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.MedicationAdministrationEntity;
import java.time.Instant;
import java.util.UUID;

public record MedicationAdministrationResponse(
    UUID id,
    UUID hospitalizationId,
    UUID prescriptionItemId,
    String medicationName,
    String dose,
    String administeredBy,
    Instant administeredAt,
    Instant createdAt
) {
    public static MedicationAdministrationResponse fromEntity(MedicationAdministrationEntity entity) {
        return new MedicationAdministrationResponse(
            entity.getId(),
            entity.getHospitalizationId(),
            entity.getPrescriptionItemId(),
            entity.getMedicationName(),
            entity.getDose(),
            entity.getAdministeredBy(),
            entity.getAdministeredAt(),
            entity.getCreatedAt()
        );
    }
}
