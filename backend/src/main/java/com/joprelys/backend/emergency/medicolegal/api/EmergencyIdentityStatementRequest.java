package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EmergencyIdentityStatementRequest(
        @NotBlank(message = "EMERGENCY_IDENTITY_FIELD_REQUIRED")
        @Size(max = 80, message = "EMERGENCY_IDENTITY_FIELD_TOO_LONG")
        String fieldName,

        @NotBlank(message = "EMERGENCY_IDENTITY_VALUE_REQUIRED")
        @Size(max = 1000, message = "EMERGENCY_IDENTITY_VALUE_TOO_LONG")
        String value,

        @NotNull(message = "EMERGENCY_IDENTITY_CONFIDENCE_REQUIRED")
        IdentityConfidenceLevel confidenceLevel,

        @Size(max = 255, message = "EMERGENCY_IDENTITY_PROOF_TOO_LONG")
        String proofReference,

        Instant declaredAt) {
}
