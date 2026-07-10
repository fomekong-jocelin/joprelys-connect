package com.joprelys.backend.cash.application;

import com.joprelys.backend.cash.api.CashSessionSummaryResponse;
import com.joprelys.backend.cash.infrastructure.persistence.CashMovementEntity;
import com.joprelys.backend.cash.infrastructure.persistence.CashRegisterSessionEntity;

import java.util.List;

/** Computes the physical-cash reconciliation without treating cheques or transfers as cash. */
public final class CashSessionReconciliationCalculator {

    private CashSessionReconciliationCalculator() {
    }

    public static CashSessionSummaryResponse calculate(CashRegisterSessionEntity session,
                                                        List<CashMovementEntity> movements) {
        double cashReceipts = total(movements, "IN", "CASH");
        double chequeReceipts = total(movements, "IN", "CHECK");
        double transferReceipts = total(movements, "IN", "BANK_TRANSFER");
        double cashExpenses = total(movements, "OUT", "CASH");
        double bankDeposits = movements.stream()
                .filter(movement -> "TRANSFER_TO_BANK".equalsIgnoreCase(movement.getMovementType()))
                .mapToDouble(CashMovementEntity::getAmount)
                .sum();
        double expectedCash = session.getOpeningBalance() + cashReceipts - cashExpenses - bankDeposits;

        return new CashSessionSummaryResponse(
                session.getId(),
                session.getOpeningBalance(),
                cashReceipts,
                chequeReceipts,
                transferReceipts,
                cashExpenses,
                bankDeposits,
                expectedCash
        );
    }

    private static double total(List<CashMovementEntity> movements, String type, String method) {
        return movements.stream()
                .filter(movement -> type.equalsIgnoreCase(movement.getMovementType()))
                .filter(movement -> method.equalsIgnoreCase(movement.getPaymentMethod()))
                .mapToDouble(CashMovementEntity::getAmount)
                .sum();
    }
}
