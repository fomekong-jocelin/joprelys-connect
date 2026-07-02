package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RequestOtpRequest(
        @NotBlank(message = "Le numéro DPU est obligatoire")
        String globalPatientNumber,

        @NotBlank(message = "Le numéro de téléphone est obligatoire")
        String phone,

        @NotNull(message = "La date de naissance est obligatoire")
        LocalDate birthDate
) {
}
