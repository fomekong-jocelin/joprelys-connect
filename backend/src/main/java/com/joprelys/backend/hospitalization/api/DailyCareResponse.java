package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.infrastructure.persistence.HospitalizationDailyCareEntity;
import java.time.Instant;
import java.util.UUID;

public record DailyCareResponse(
    UUID id,
    UUID hospitalizationId,
    String careType,
    String description,
    boolean billable,
    Double price,
    String performedBy,
    Instant performedAt,
    Instant createdAt
) {
    public static DailyCareResponse fromEntity(HospitalizationDailyCareEntity entity) {
        return new DailyCareResponse(
            entity.getId(),
            entity.getHospitalizationId(),
            entity.getCareType(),
            entity.getDescription(),
            entity.isBillable(),
            entity.getPrice(),
            entity.getPerformedBy(),
            entity.getPerformedAt(),
            entity.getCreatedAt()
        );
    }
}
