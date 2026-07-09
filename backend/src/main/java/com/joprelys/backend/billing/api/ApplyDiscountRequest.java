package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ApplyDiscountRequest(
    @Min(value = 0, message = "Le montant de la remise doit être positif")
    double discountAmount,
    @NotBlank(message = "Le motif de la remise est obligatoire")
    String discountReason
) {}
