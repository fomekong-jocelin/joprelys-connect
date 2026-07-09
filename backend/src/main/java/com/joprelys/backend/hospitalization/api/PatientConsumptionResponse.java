package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.PatientConsumptionEntity;
import java.time.Instant;
import java.util.UUID;

public record PatientConsumptionResponse(
    UUID id,
    UUID hospitalizationId,
    String itemName,
    int quantity,
    double unitPrice,
    String consumedBy,
    Instant consumedAt,
    Instant createdAt
) {
    public static PatientConsumptionResponse fromEntity(PatientConsumptionEntity entity) {
        return new PatientConsumptionResponse(
            entity.getId(),
            entity.getHospitalizationId(),
            entity.getItemName(),
            entity.getQuantity(),
            entity.getUnitPrice(),
            entity.getConsumedBy(),
            entity.getConsumedAt(),
            entity.getCreatedAt()
        );
    }
}
