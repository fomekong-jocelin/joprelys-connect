package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import java.time.Instant;
import java.util.UUID;

public record BedAssignmentResponse(
        UUID id,
        UUID hospitalizationId,
        UUID bedId,
        Instant assignedAt,
        Instant releasedAt
) {
    public static BedAssignmentResponse fromEntity(BedAssignmentEntity entity) {
        return new BedAssignmentResponse(
                entity.getId(),
                entity.getHospitalizationId(),
                entity.getBed().getId(),
                entity.getAssignedAt(),
                entity.getReleasedAt()
        );
    }
}
