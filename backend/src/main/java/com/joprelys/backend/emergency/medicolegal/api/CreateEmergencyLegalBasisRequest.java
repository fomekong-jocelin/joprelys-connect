package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyLegalBasisType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public record CreateEmergencyLegalBasisRequest(
        @NotNull(message = "EMERGENCY_LEGAL_BASIS_TYPE_REQUIRED")
        EmergencyLegalBasisType basisType,

        @NotBlank(message = "EMERGENCY_LEGAL_BASIS_JUSTIFICATION_REQUIRED")
        @Size(max = 1500, message = "EMERGENCY_LEGAL_BASIS_JUSTIFICATION_TOO_LONG")
        String justification,

        Instant startsAt,

        Instant expiresAt,

        @NotEmpty(message = "EMERGENCY_LEGAL_BASIS_ACT_REQUIRED")
        Set<@NotBlank(message = "EMERGENCY_LEGAL_BASIS_ACT_REQUIRED")
                @Size(max = 255, message = "EMERGENCY_LEGAL_BASIS_ACT_TOO_LONG") String> coveredActs) {

    @AssertTrue(message = "EMERGENCY_LEGAL_BASIS_EXPIRY_INVALID")
    public boolean isExpiryAfterStart() {
        if (expiresAt == null) return true;
        Instant effectiveStart = startsAt == null ? Instant.now() : startsAt;
        return expiresAt.isAfter(effectiveStart);
    }
}
