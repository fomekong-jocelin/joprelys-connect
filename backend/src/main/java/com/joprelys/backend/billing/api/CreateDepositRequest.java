package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateDepositRequest(
        @NotNull(message = "Le patient est obligatoire.") UUID patientId,
        UUID visitId,
        @NotNull(message = "Le montant est obligatoire.")
        @Positive(message = "Le montant doit être supérieur à zéro.") BigDecimal amount,
        @NotNull(message = "Le mode d'encaissement est obligatoire.") PaymentMethod method,
        @Size(max = 100) String reference,
        @Size(max = 500) String note
) {}
