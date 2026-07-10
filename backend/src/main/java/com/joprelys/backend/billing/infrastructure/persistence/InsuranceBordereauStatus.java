package com.joprelys.backend.billing.infrastructure.persistence;

public enum InsuranceBordereauStatus {
    DRAFT,
    SENT,
    RECEIVED,
    ACCEPTED,
    PARTIALLY_PAID,
    SETTLED,
    REJECTED,
    CANCELLED,
    /** Valeur historique conservée en lecture pour compatibilité. */
    PAID
}
