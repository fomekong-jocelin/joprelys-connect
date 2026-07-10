package com.joprelys.backend.billing.api;

import java.util.UUID;

/** Read model used by billing screens to present patient and insurance collections clearly. */
public record InvoiceSettlementSummaryResponse(
        UUID invoiceId,
        String collectionStatus,
        SettlementPartyResponse patient,
        SettlementPartyResponse insurance
) {
}
