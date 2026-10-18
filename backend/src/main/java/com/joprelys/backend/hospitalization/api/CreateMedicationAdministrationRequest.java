package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public record CreateMedicationAdministrationRequest(
    @jakarta.validation.constraints.NotNull(message = "Une prescription validée est obligatoire")
    UUID prescriptionItemId,
    @NotBlank(message = "Le nom du médicament est obligatoire")
    String medicationName,
    @NotBlank(message = "La dose est obligatoire")
    String dose,
    Instant administeredAt,
    /** Clé générée par le client : un double envoi renvoie l'administration déjà tracée. */
    UUID clientRequestId,
    /** Motif obligatoire pour administrer dans la fenêtre de sécurité d'une administration précédente. */
    @jakarta.validation.constraints.Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    String overrideReason
) {
    public CreateMedicationAdministrationRequest(UUID prescriptionItemId, String medicationName, String dose, Instant administeredAt) {
        this(prescriptionItemId, medicationName, dose, administeredAt, null, null);
    }
}
