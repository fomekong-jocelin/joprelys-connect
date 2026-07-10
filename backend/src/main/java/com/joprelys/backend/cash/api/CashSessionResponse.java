package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;
import java.time.Instant;
import java.util.UUID;

public record CashSessionResponse(
    UUID id,
    UUID cashRegisterId,
    String cashRegisterName,
    UUID openedByUserId,
    Instant openedAt,
    Double openingBalance,
    UUID closedByUserId,
    Instant closedAt,
    Double closingBalance,
    Double declaredBalance,
    Double discrepancyAmount,
    String discrepancyReason,
    String status,
    Boolean discrepancyResolved,
    String resolutionNotes,
    String resolvedByUserId,
    Instant resolvedAt
) {
    public static CashSessionResponse fromEntity(CashRegisterSessionEntity entity) {
        return new CashSessionResponse(
            entity.getId(),
            entity.getCashRegister().getId(),
            entity.getCashRegister().getName(),
            entity.getOpenedByUserId(),
            entity.getOpenedAt(),
            entity.getOpeningBalance(),
            entity.getClosedByUserId(),
            entity.getClosedAt(),
            entity.getClosingBalance(),
            entity.getDeclaredBalance(),
            entity.getDiscrepancyAmount(),
            entity.getDiscrepancyReason(),
            entity.getStatus(),
            entity.getDiscrepancyResolved(),
            entity.getResolutionNotes(),
            entity.getResolvedByUserId(),
            entity.getResolvedAt()
        );
    }
}
