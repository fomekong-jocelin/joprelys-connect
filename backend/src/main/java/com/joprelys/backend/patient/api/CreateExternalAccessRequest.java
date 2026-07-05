package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateExternalAccessRequest(
        @NotBlank(message = "Le numéro DPU du patient est obligatoire.")
        @Size(min = 5, max = 50, message = "Le numéro DPU doit contenir entre 5 et 50 caractères.")
        String patientDpu,

        @NotBlank(message = "Le motif de la demande est obligatoire.")
        @Size(min = 10, max = 500, message = "Le motif doit contenir entre 10 et 500 caractères.")
        String reason,

        @NotNull(message = "La durée d'accès est obligatoire.")
        @Min(value = 1, message = "La durée minimale est de 1 heure.")
        @Max(value = 168, message = "La durée maximale est de 168 heures (7 jours).")
        Integer durationHours,
        String scopes
) {}
