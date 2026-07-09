package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record CreatePatientConsumptionRequest(
    @NotBlank(message = "Le nom de l'article est obligatoire")
    String itemName,
    @Min(value = 1, message = "La quantité doit être supérieure ou égale à 1")
    int quantity,
    double unitPrice,
    Instant consumedAt
) {}
