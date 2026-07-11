package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import java.time.Instant;
import java.util.UUID;

public record ProvisionalPatientDetailsResponse(
        UUID id,
        UUID organizationId,
        String globalPatientNumber,
        String localPatientNumber,
        PatientIdentityStatus identityStatus,
        String temporaryPatientNumber,
        String displayName,
        IdentityConfidenceLevel identityConfidenceLevel,
        String apparentGender,
        String estimatedAgeRange,
        String physicalDescription,
        Instant foundAt,
        String foundLocation,
        Instant createdAt,
        Instant updatedAt) {
}
