package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.infrastructure.persistence.CashMovementEntity;
import java.time.Instant;
import java.util.UUID;

public record CashMovementResponse(
    UUID id,
    UUID cashRegisterSessionId,
    String movementType,
    Double amount,
    String description,
    String paymentMethod,
    String referenceNumber,
    UUID createdByUserId,
    Instant createdAt
) {
    public static CashMovementResponse fromEntity(CashMovementEntity entity) {
        return new CashMovementResponse(
            entity.getId(),
            entity.getCashRegisterSession().getId(),
            entity.getMovementType(),
            entity.getAmount(),
            entity.getDescription(),
            entity.getPaymentMethod(),
            entity.getReferenceNumber(),
            entity.getCreatedByUserId(),
            entity.getCreatedAt()
        );
    }
}
