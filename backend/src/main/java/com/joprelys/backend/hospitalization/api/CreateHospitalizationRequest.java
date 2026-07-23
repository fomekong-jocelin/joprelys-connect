package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateHospitalizationRequest(
        @NotNull(message = "L'identifiant du patient est obligatoire.")
        UUID patientId,
        @NotNull(message = "L'unité de service est obligatoire.")
        UUID serviceUnitId,
        @NotNull(message = "L'espace d'hébergement est obligatoire.")
        UUID spaceId,
        @NotNull(message = "Le lit est obligatoire.")
        UUID bedId,
        @NotBlank(message = "Le motif d'admission est obligatoire.")
        String admissionReason,
        UUID visitId,
        UUID emergencyId,
        @NotNull(message = "Le médecin responsable est obligatoire.")
        UUID responsiblePractitionerId
) {
    @AssertTrue(message = "Une visite ou une urgence associée est obligatoire.")
    public boolean hasCareContext() {
        return visitId != null || emergencyId != null;
    }
}
