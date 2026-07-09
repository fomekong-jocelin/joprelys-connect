package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;

public record EstimateItemRequest(
    @NotBlank(message = "Le libellé est obligatoire")
    String label,
    @NotBlank(message = "Le type de produit/acte est obligatoire")
    String itemType,
    @Min(value = 0, message = "Le prix unitaire doit être positif")
    double unitPrice,
    @Min(value = 0, message = "La quantité doit être positive")
    double quantity
) {}
