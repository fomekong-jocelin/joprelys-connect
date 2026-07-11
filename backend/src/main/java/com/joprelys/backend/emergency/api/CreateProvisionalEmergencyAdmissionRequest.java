package com.joprelys.backend.emergency.api;

import com.joprelys.backend.patient.api.CreateProvisionalPatientRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateProvisionalEmergencyAdmissionRequest(
        @NotNull(message = "EMERGENCY_ADMISSION_REQUEST_ID_REQUIRED")
        UUID requestId,

        @NotNull(message = "EMERGENCY_PROVISIONAL_PATIENT_REQUIRED")
        @Valid
        CreateProvisionalPatientRequest patient,

        @NotNull(message = "EMERGENCY_TRIAGE_REQUIRED")
        @Valid
        EmergencyTriageRequest emergency) {
}
