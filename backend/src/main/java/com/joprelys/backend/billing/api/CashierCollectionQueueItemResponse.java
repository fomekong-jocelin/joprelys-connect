package com.joprelys.backend.billing.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Minimal, tenant-scoped work item exposed to the cashier workspace.
 * No clinical detail or insurance collection data is included.
 */
public record CashierCollectionQueueItemResponse(
        UUID invoiceId,
        String invoiceNumber,
        UUID patientId,
        String patientName,
        String globalPatientNumber,
        String phone,
        BigDecimal totalAmount,
        BigDecimal patientRemainingAmount,
        InvoiceCollectionStatus collectionStatus,
        Instant createdAt,
        Instant validatedAt
) {
}
