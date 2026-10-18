package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/** Imputation d'un acompte ; sans montant, le solde imputable est utilisé. */
public record ApplyDepositRequest(
        @NotNull(message = "L'acompte est obligatoire.") UUID depositId,
        @Positive(message = "Le montant doit être supérieur à zéro.") BigDecimal amount
) {}
