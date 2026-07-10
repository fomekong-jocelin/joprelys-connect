package com.joprelys.backend.billing.infrastructure.persistence;

public enum InvoiceStatus {
    PENDING,
    PARTIALLY_PAID,
    /** Part patient soldée, part assurance encore à recouvrer. */
    PAID,
    /** Toutes les obligations patient et assurance sont soldées. */
    SETTLED,
    PROFORMA,
    VALIDATED,
    CANCELLED
}
