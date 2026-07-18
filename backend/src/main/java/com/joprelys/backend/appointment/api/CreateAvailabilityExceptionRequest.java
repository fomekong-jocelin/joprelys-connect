package com.joprelys.backend.appointment.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload de création d'une indisponibilité ponctuelle (congé, absence).
 * {@code doctorId} omis = médecin connecté ; un médecin ne peut viser que lui-même.
 */
public record CreateAvailabilityExceptionRequest(
		UUID doctorId,

		@NotNull(message = "La date de début de l'indisponibilité est obligatoire.")
		Instant startAt,

		@NotNull(message = "La date de fin de l'indisponibilité est obligatoire.")
		Instant endAt,

		@Size(max = 255, message = "Le motif ne doit pas dépasser 255 caractères.")
		String reason
) {
}
