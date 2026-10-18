package com.joprelys.backend.billing.infrastructure.persistence;

public enum PaymentMethod {
    CASH,
    CHECK,
    BANK_TRANSFER,
    /** Imputation d'un acompte déjà encaissé : aucun nouveau mouvement de caisse. */
    DEPOSIT
}
