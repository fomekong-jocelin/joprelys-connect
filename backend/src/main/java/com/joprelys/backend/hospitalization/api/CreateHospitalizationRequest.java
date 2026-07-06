package com.joprelys.backend.hospitalization.api;

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
        @NotNull(message = "La visite associée est obligatoire.")
        UUID visitId,
        @NotNull(message = "Le médecin responsable est obligatoire.")
        UUID responsiblePractitionerId
) {}
