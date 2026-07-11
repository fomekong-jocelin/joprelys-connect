package com.joprelys.backend.emergency.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EmergencyTriageRequest(
        @NotBlank(message = "EMERGENCY_ARRIVAL_MODE_REQUIRED")
        @Size(max = 50, message = "EMERGENCY_ARRIVAL_MODE_TOO_LONG")
        String arrivalMode,

        @NotBlank(message = "EMERGENCY_TRIAGE_LEVEL_REQUIRED")
        @Size(max = 20, message = "EMERGENCY_TRIAGE_LEVEL_TOO_LONG")
        String triageLevel,

        @NotBlank(message = "EMERGENCY_HEMODYNAMIC_STATUS_REQUIRED")
        @Size(max = 50, message = "EMERGENCY_HEMODYNAMIC_STATUS_TOO_LONG")
        String hemodynamicStatus,

        @NotBlank(message = "EMERGENCY_CHIEF_COMPLAINT_REQUIRED")
        @Size(max = 4000, message = "EMERGENCY_CHIEF_COMPLAINT_TOO_LONG")
        String chiefComplaint,

        Integer initialBpSystolic,
        Integer initialBpDiastolic,
        Integer initialHr,
        BigDecimal initialTemp,

        @Size(max = 160, message = "EMERGENCY_THIRD_PARTY_NAME_TOO_LONG")
        String thirdPartyName,

        @Size(max = 40, message = "EMERGENCY_THIRD_PARTY_PHONE_TOO_LONG")
        String thirdPartyPhone,

        @Size(max = 80, message = "EMERGENCY_THIRD_PARTY_RELATIONSHIP_TOO_LONG")
        String thirdPartyRelationship,

        @Size(max = 120, message = "EMERGENCY_THIRD_PARTY_DOCUMENT_TOO_LONG")
        String thirdPartyIdDocument,

        @Size(max = 1000, message = "EMERGENCY_THIRD_PARTY_CIRCUMSTANCES_TOO_LONG")
        String thirdPartyCircumstances,

        Boolean thirdPartyConsentToContact) {

    @AssertTrue(message = "EMERGENCY_THIRD_PARTY_REQUIRED")
    public boolean isAccompanyingPersonComplete() {
        if (!"ACCOMPANIED".equalsIgnoreCase(arrivalMode)) {
            return true;
        }
        return hasText(thirdPartyName)
                && hasText(thirdPartyPhone)
                && hasText(thirdPartyRelationship);
    }

    public CreateEmergencyRequest forPatient(java.util.UUID patientId) {
        return new CreateEmergencyRequest(
                patientId,
                arrivalMode,
                triageLevel,
                hemodynamicStatus,
                chiefComplaint,
                initialBpSystolic,
                initialBpDiastolic,
                initialHr,
                initialTemp,
                thirdPartyName,
                thirdPartyPhone,
                thirdPartyRelationship,
                thirdPartyIdDocument,
                thirdPartyCircumstances,
                thirdPartyConsentToContact);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
