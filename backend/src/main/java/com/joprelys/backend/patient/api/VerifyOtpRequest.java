package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;

public record VerifyOtpRequest(
        @NotBlank(message = "Le numéro DPU est obligatoire")
        String globalPatientNumber,

        @NotBlank(message = "Le code de sécurité est obligatoire")
        String otpCode
) {
}
