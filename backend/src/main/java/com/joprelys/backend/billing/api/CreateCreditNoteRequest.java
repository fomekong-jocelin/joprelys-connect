package com.joprelys.backend.billing.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCreditNoteRequest(
    @Min(value = 0, message = "Le montant doit être positif")
    double amount,
    @NotBlank(message = "Le motif de l'avoir est obligatoire")
    String reason
) {}
