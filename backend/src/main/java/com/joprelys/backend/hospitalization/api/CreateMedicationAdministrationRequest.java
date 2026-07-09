package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public record CreateMedicationAdministrationRequest(
    UUID prescriptionItemId,
    @NotBlank(message = "Le nom du médicament est obligatoire")
    String medicationName,
    @NotBlank(message = "La dose est obligatoire")
    String dose,
    Instant administeredAt
) {}
