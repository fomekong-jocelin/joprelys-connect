package com.joprelys.backend.billing.api;

import com.joprelys.backend.billing.infrastructure.persistence.InvoiceItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvoiceItemDto(
        @NotBlank(message = "Le libellé de l'acte est obligatoire.")
        String label,

        @NotNull(message = "Le type d'acte est obligatoire.")
        InvoiceItemType itemType,

        @NotNull(message = "Le prix unitaire est obligatoire.")
        Double unitPrice,

        @NotNull(message = "La quantité est obligatoire.")
        Double quantity,

        Double coefficient
) {}
