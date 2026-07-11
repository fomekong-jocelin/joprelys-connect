package com.joprelys.backend.emergency.medicolegal.api;

import com.joprelys.backend.emergency.medicolegal.domain.EmergencyInformationSourceType;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyThirdPartyQuality;
import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record CreateEmergencyThirdPartyRequest(
        @NotBlank(message = "EMERGENCY_THIRD_PARTY_NAME_REQUIRED")
        @Size(max = 160, message = "EMERGENCY_THIRD_PARTY_NAME_TOO_LONG")
        String fullName,

        @Size(max = 40, message = "EMERGENCY_THIRD_PARTY_PHONE_TOO_LONG")
        String phone,

        @Email(message = "EMERGENCY_THIRD_PARTY_EMAIL_INVALID")
        @Size(max = 255, message = "EMERGENCY_THIRD_PARTY_EMAIL_TOO_LONG")
        String email,

        @Size(max = 120, message = "EMERGENCY_THIRD_PARTY_ID_TOO_LONG")
        String idDocument,

        @Size(max = 80, message = "EMERGENCY_THIRD_PARTY_RELATIONSHIP_TOO_LONG")
        String relationshipToPatient,

        @Size(max = 1000, message = "EMERGENCY_THIRD_PARTY_CIRCUMSTANCES_TOO_LONG")
        String circumstances,

        boolean consentToContact,

        boolean legalRepresentativeClaimed,

        @NotNull(message = "EMERGENCY_THIRD_PARTY_SOURCE_REQUIRED")
        EmergencyInformationSourceType sourceType,

        @NotNull(message = "EMERGENCY_THIRD_PARTY_CONFIDENCE_REQUIRED")
        IdentityConfidenceLevel confidenceLevel,

        @Size(max = 255, message = "EMERGENCY_THIRD_PARTY_PROOF_TOO_LONG")
        String proofReference,

        @NotEmpty(message = "EMERGENCY_THIRD_PARTY_QUALITY_REQUIRED")
        Set<EmergencyThirdPartyQuality> qualities,

        @Valid
        List<EmergencyIdentityStatementRequest> identityStatements) {
}
