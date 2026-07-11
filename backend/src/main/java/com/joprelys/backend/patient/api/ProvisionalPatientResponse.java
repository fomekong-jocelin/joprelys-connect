package com.joprelys.backend.patient.api;

import java.util.List;

public record ProvisionalPatientResponse(
        ProvisionalPatientDetailsResponse patient,
        List<IdentityDeclarationResponse> identityDeclarations) {
}
