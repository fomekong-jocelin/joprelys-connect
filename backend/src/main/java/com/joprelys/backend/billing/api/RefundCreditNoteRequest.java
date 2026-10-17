package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RefundCreditNoteRequest(
        @NotNull(message = "Le montant remboursé est obligatoire.")
        @Positive(message = "Le montant remboursé doit être supérieur à zéro.")
        BigDecimal amount,
        @NotNull(message = "Le mode de remboursement est obligatoire.")
        PaymentMethod method,
        @Size(max = 100, message = "La référence ne peut pas dépasser 100 caractères.")
        String reference
) {}
