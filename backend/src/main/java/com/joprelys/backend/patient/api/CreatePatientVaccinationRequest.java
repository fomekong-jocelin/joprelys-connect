package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreatePatientVaccinationRequest(
        @NotBlank(message = "Le nom du vaccin est obligatoire")
        String vaccineName,

        String batchNumber,

        @NotNull(message = "La date d'administration est obligatoire")
        LocalDate administeredAt,

        String administeredBy,
        String notes,
        LocalDate nextDoseAt
) {
}
