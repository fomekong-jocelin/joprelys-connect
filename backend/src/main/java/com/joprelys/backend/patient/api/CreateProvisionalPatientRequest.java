package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentityConfidenceLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record CreateProvisionalPatientRequest(
        @Size(max = 32) String apparentGender,
        @Size(max = 64) String estimatedAgeRange,
        @Size(max = 2000) String physicalDescription,
        Instant foundAt,
        @Size(max = 255) String foundLocation,
        IdentityConfidenceLevel confidenceLevel,
        @Valid @Size(max = 25) List<IdentityDeclarationRequest> identityDeclarations) {
}
