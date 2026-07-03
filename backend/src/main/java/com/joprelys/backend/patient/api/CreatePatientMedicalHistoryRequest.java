package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePatientMedicalHistoryRequest(
        @NotBlank(message = "La catégorie est obligatoire.")
        String category,
        @NotBlank(message = "La description est obligatoire.")
        String description,
        LocalDate onsetDate,
        boolean isOngoing,
        String comment
) {}
