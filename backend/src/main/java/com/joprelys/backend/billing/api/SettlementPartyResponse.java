package com.joprelys.backend.billing.api;

/** Amounts and settlement status for one invoice debtor. */
public record SettlementPartyResponse(
        Double totalAmount,
        Double paidAmount,
        Double remainingAmount,
        String status
) {
}
