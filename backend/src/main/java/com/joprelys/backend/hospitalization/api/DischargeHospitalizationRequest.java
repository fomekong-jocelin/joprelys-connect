package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;

public record DischargeHospitalizationRequest(
        @NotBlank(message = "Le diagnostic final de sortie est obligatoire.")
        String dischargeDiagnosis,
        @NotBlank(message = "Les consignes de sortie sont obligatoires.")
        String dischargeInstructions
) {}
