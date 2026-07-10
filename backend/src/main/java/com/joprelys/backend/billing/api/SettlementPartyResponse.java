package com.joprelys.backend.billing.api;

import java.math.BigDecimal;

/** Amounts and settlement status for one invoice debtor. */
public record SettlementPartyResponse(
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String status
) {
}
