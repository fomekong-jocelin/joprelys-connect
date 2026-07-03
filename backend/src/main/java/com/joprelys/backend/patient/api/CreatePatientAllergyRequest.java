package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePatientAllergyRequest(
        @NotBlank(message = "La substance est obligatoire.")
        String substance,
        @NotBlank(message = "La gravité est obligatoire.")
        String severity,
        String reaction,
        String status,
        LocalDate discoveredAt,
        String comment
) {}
