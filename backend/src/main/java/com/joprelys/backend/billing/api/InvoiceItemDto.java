package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record InvoiceItemDto(
        @NotBlank(message = "Le libellé de l'acte est obligatoire.")
        String label,

        @NotNull(message = "Le type d'acte est obligatoire.")
        InvoiceItemType itemType,

        @NotNull(message = "Le prix unitaire est obligatoire.")
        @Positive(message = "Le prix unitaire doit être supérieur à zéro.")
        BigDecimal unitPrice,

        @NotNull(message = "La quantité est obligatoire.")
        @Positive(message = "La quantité doit être supérieure à zéro.")
        BigDecimal quantity,

        BigDecimal coefficient
) {}
