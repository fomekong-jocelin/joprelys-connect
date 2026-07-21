package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateHospitalizationRequest(
        @NotNull(message = "L'identifiant du patient est obligatoire.")
        UUID patientId,
        @NotBlank(message = "Le nom du service est obligatoire.")
        String serviceName,
        @NotBlank(message = "Le numéro de chambre est obligatoire.")
        String roomNumber,
        @NotBlank(message = "Le numéro de lit est obligatoire.")
        String bedNumber,
        @NotBlank(message = "Le motif d'admission est obligatoire.")
        String admissionReason,
        UUID visitId,
        UUID emergencyId,
        @NotNull(message = "Le médecin responsable est obligatoire.")
        UUID responsiblePractitionerId
) {
    /** Backward-compatible constructor for the existing visit-based admission flow. */
    public CreateHospitalizationRequest(
            UUID patientId,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String admissionReason,
            UUID visitId,
            UUID responsiblePractitionerId) {
        this(
                patientId,
                serviceName,
                roomNumber,
                bedNumber,
                admissionReason,
                visitId,
                null,
                responsiblePractitionerId);
    }

    @AssertTrue(message = "Une visite ou une urgence associée est obligatoire.")
    public boolean hasCareContext() {
        return visitId != null || emergencyId != null;
    }
}
