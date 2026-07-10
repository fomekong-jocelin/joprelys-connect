package com.joprelys.backend.billing.api;

/**
 * État métier consolidé du recouvrement d'une facture.
 *
 * <p>Ce statut est dérivé des créances patient et assurance. Il ne remplace pas
 * le cycle de vie persistant de la facture, mais constitue son interprétation
 * unique pour les API et les écrans financiers.</p>
 */
public enum InvoiceCollectionStatus {
    NOT_YET_DUE,
    PATIENT_DUE,
    PATIENT_PARTIALLY_PAID,
    INSURANCE_DUE,
    SETTLED,
    CANCELLED
}
