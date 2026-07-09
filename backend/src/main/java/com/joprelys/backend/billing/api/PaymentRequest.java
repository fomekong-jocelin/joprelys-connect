package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotNull(message = "Le montant du règlement est obligatoire.")
        @Positive(message = "Le montant du règlement doit être supérieur à zéro.")
        Double amount,

        @NotNull(message = "Le mode de règlement est obligatoire.")
        PaymentMethod method,

        String reference
) {}
