package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import java.util.UUID;

public record BedResponse(
        UUID id,
        UUID roomId,
        String bedNumber,
        String status,
        Integer version
) {
    public static BedResponse fromEntity(BedEntity entity) {
        return new BedResponse(
                entity.getId(),
                entity.getRoom().getId(),
                entity.getBedNumber(),
                entity.getStatus().name(),
                entity.getVersion()
        );
    }
}
