package com.joprelys.backend.cash.api;

import java.util.UUID;

/** Reconciliation totals for a cash session; expected cash excludes cheques and transfers. */
public record CashSessionSummaryResponse(
        UUID sessionId,
        Double openingCash,
        Double cashReceipts,
        Double chequeReceipts,
        Double transferReceipts,
        Double cashExpenses,
        Double bankDeposits,
        Double expectedCash
) {
}
