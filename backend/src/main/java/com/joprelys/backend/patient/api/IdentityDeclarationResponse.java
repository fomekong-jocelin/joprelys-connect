package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.IdentityVerificationStatus;
import java.time.Instant;
import java.util.UUID;

public record IdentityDeclarationResponse(
        UUID id,
        String fieldName,
        String value,
        IdentitySourceType sourceType,
        String sourceDetails,
        IdentityConfidenceLevel confidenceLevel,
        IdentityVerificationStatus verificationStatus,
        UUID declaredBy,
        Instant declaredAt) {
}
