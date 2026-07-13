package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.triage.api.EmergencyAbcdeAssessmentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateEmergencyRequest(
        @NotNull(message = "L'identifiant du patient est obligatoire.")
        UUID patientId,

        @NotBlank(message = "Le mode d'arrivée est obligatoire.")
        @Size(max = 50)
        String arrivalMode,

        @NotBlank(message = "Le niveau de triage est obligatoire.")
        @Size(max = 20)
        String triageLevel,

        @NotBlank(message = "L'état hémodynamique est obligatoire.")
        @Size(max = 50)
        String hemodynamicStatus,

        @NotBlank(message = "Le motif est obligatoire.")
        String chiefComplaint,

        Integer initialBpSystolic,

        Integer initialBpDiastolic,

        Integer initialHr,

        BigDecimal initialTemp,

        @Size(max = 160)
        String thirdPartyName,

        @Size(max = 40)
        String thirdPartyPhone,

        @Size(max = 80)
        String thirdPartyRelationship,

        @Size(max = 120)
        String thirdPartyIdDocument,

        @Size(max = 1000)
        String thirdPartyCircumstances,

        Boolean thirdPartyConsentToContact,

        @Valid
        EmergencyAbcdeAssessmentRequest abcdeAssessment
) {

    /**
     * Constructeur de compatibilité pour les appels antérieurs au triage ABCDE.
     */
    public CreateEmergencyRequest(
            UUID patientId,
            String arrivalMode,
            String triageLevel,
            String hemodynamicStatus,
            String chiefComplaint,
            Integer initialBpSystolic,
            Integer initialBpDiastolic,
            Integer initialHr,
            BigDecimal initialTemp,
            String thirdPartyName,
            String thirdPartyPhone,
            String thirdPartyRelationship,
            String thirdPartyIdDocument,
            String thirdPartyCircumstances,
            Boolean thirdPartyConsentToContact) {
        this(
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
                thirdPartyConsentToContact,
                null);
    }

    /**
     * Constructeur de compatibilité pour les appels internes et tests antérieurs
     * à l'ajout du déclarant/accompagnant.
     */
    public CreateEmergencyRequest(
            UUID patientId,
            String arrivalMode,
            String triageLevel,
            String hemodynamicStatus,
            String chiefComplaint,
            Integer initialBpSystolic,
            Integer initialBpDiastolic,
            Integer initialHr,
            BigDecimal initialTemp) {
        this(
                patientId,
                arrivalMode,
                triageLevel,
                hemodynamicStatus,
                chiefComplaint,
                initialBpSystolic,
                initialBpDiastolic,
                initialHr,
                initialTemp,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                null);
    }

    @AssertTrue(message = "Le nom, le téléphone et le lien avec le patient sont obligatoires lorsqu'un tiers amène le patient.")
    public boolean isAccompanyingPersonComplete() {
        if (!"ACCOMPANIED".equalsIgnoreCase(arrivalMode)) {
            return true;
        }
        return hasText(thirdPartyName)
                && hasText(thirdPartyPhone)
                && hasText(thirdPartyRelationship);
    }

    public boolean contactConsentGranted() {
        return Boolean.TRUE.equals(thirdPartyConsentToContact);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}