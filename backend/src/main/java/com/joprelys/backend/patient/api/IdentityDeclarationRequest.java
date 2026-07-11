package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record IdentityDeclarationRequest(
        @NotBlank @Size(max = 80) String fieldName,
        @NotBlank @Size(max = 2000) String value,
        @NotNull IdentitySourceType sourceType,
        @Size(max = 500) String sourceDetails,
        @NotNull IdentityConfidenceLevel confidenceLevel,
        Instant declaredAt) {
}
