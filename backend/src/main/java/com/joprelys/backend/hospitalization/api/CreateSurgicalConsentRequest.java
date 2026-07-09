package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;

public record CreateSurgicalConsentRequest(
        @NotBlank(message = "Le type de consentement est obligatoire.")
        String consentType,
        boolean patientSignaturePresent,
        String witnessName
) {}
