package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.ReceivableReminderEntity;
import java.time.Instant;
import java.util.UUID;

public record ReceivableReminderResponse(
    UUID id,
    UUID receivableId,
    String actionType,
    String status,
    String notes,
    UUID actorId,
    Instant createdAt
) {
    public static ReceivableReminderResponse fromEntity(ReceivableReminderEntity entity) {
        return new ReceivableReminderResponse(
            entity.getId(),
            entity.getReceivableId(),
            entity.getActionType().name(),
            entity.getStatus().name(),
            entity.getNotes(),
            entity.getActorId(),
            entity.getCreatedAt()
        );
    }
}
