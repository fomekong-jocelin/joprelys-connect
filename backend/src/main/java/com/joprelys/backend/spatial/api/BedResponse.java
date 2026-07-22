package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedStatus;
import java.util.UUID;

public record BedResponse(
        UUID id,
        UUID roomId,
        String bedNumber,
        String status,
        String capacityStatus,
        String readinessStatus,
        String usageStatus,
        boolean available,
        Integer version
) {
    public static BedResponse fromEntity(BedEntity entity) {
        return fromEntity(entity, entity.getStatus() == BedStatus.OCCUPIED);
    }

    public static BedResponse fromEntity(BedEntity entity, boolean hasActiveAssignment) {
        return new BedResponse(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getBedNumber(),
                entity.getStatus().name(),
                entity.getCapacityStatus().name(),
                entity.getReadinessStatus().name(),
                hasActiveAssignment ? "OCCUPIED" : "UNASSIGNED",
                entity.isOperationallyAvailable(hasActiveAssignment),
                entity.getVersion()
        );
    }
}
