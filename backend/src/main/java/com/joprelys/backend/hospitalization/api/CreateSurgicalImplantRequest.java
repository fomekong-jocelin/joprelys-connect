package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateSurgicalImplantRequest(
    @NotBlank(message = "Le nom de l'implant est obligatoire")
    String implantName,
    String lotNumber,
    @Min(value = 1, message = "La quantité doit être supérieure ou égale à 1")
    int quantity,
    double unitPrice,
    String manufacturer
) {}
