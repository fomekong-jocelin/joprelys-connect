package com.joprelys.backend.billing.infrastructure.persistence;

public enum InvoiceStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    PROFORMA,
    VALIDATED,
    CANCELLED
}
