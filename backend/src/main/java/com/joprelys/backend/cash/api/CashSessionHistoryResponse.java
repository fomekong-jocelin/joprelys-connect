package com.joprelys.backend.cash.api;

import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;

import java.time.Instant;
import java.util.UUID;

/** Read model complet d'une session de caisse destiné à l'historique opérationnel. */
public record CashSessionHistoryResponse(
        UUID id,
        UUID cashRegisterId,
        String cashRegisterName,
        String reportNumber,
        UUID openedByUserId,
        String openedByName,
        Instant openedAt,
        UUID closedByUserId,
        String closedByName,
        Instant closedAt,
        String status,
        Double openingBalance,
        Double cashReceipts,
        Double chequeReceipts,
        Double transferReceipts,
        Double cashExpenses,
        Double bankDeposits,
        Double expectedCash,
        Double declaredBalance,
        Double discrepancyAmount,
        String discrepancyReason,
        Boolean discrepancyResolved
) {
    public static CashSessionHistoryResponse from(
            CashRegisterSessionEntity session,
            CashSessionSummaryResponse summary,
            String openedByName,
            String closedByName,
            String reportNumber) {
        String closeoutNumber = "CLOSED".equals(session.getStatus()) ? reportNumber : null;
        return new CashSessionHistoryResponse(
                session.getId(),
                session.getCashRegister().getId(),
                session.getCashRegister().getName(),
                closeoutNumber,
                session.getOpenedByUserId(),
                openedByName,
                session.getOpenedAt(),
                session.getClosedByUserId(),
                closedByName,
                session.getClosedAt(),
                session.getStatus(),
                session.getOpeningBalance(),
                summary.cashReceipts(),
                summary.chequeReceipts(),
                summary.transferReceipts(),
                summary.cashExpenses(),
                summary.bankDeposits(),
                summary.expectedCash(),
                session.getDeclaredBalance(),
                session.getDiscrepancyAmount(),
                session.getDiscrepancyReason(),
                session.getDiscrepancyResolved()
        );
    }
}