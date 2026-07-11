package com.joprelys.backend.patient.api;

import java.util.List;

public record ProvisionalPatientResponse(
        PatientResponse patient,
        List<IdentityDeclarationResponse> identityDeclarations) {
}
