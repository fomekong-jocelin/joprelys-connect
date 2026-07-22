package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.infrastructure.persistence.BedStateChangeEntity;
import java.time.Instant;
import java.util.UUID;

public record BedStateChangeResponse(
        UUID id,
        UUID bedId,
        String axis,
        String previousValue,
        String newValue,
        String reasonCode,
        String reasonNote,
        UUID actorId,
        String actorDisplayName,
        String source,
        Instant occurredAt
) {
    public static BedStateChangeResponse fromEntity(BedStateChangeEntity entity) {
        return new BedStateChangeResponse(
                entity.getId(),
                entity.getBedId(),
                entity.getAxis().name(),
                entity.getPreviousValue(),
                entity.getNewValue(),
                entity.getReasonCode().name(),
                entity.getReasonNote(),
                entity.getActorId(),
                entity.getActorDisplayName(),
                entity.getSource().name(),
                entity.getOccurredAt());
    }
}
