package com.joprelys.backend.billing.domain;

/**
 * Indicates whether the debtor and coverage split are definitive.
 * The financial document itself remains immutable through identity reconciliation.
 */
public enum InvoiceRegularizationStatus {
    RESOLVED,
    REGULARIZATION_PENDING
}
