package com.joprelys.backend.appointment.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Payload de création/modification d'une règle de disponibilité récurrente.
 * {@code doctorId} omis = médecin connecté ; un médecin ne peut viser que lui-même.
 */
public record UpsertAvailabilityRuleRequest(
		UUID doctorId,

		@NotNull(message = "Le jour de la semaine est obligatoire.")
		@Min(value = 1, message = "Le jour de la semaine doit être compris entre 1 (lundi) et 7 (dimanche).")
		@Max(value = 7, message = "Le jour de la semaine doit être compris entre 1 (lundi) et 7 (dimanche).")
		Integer weekday,

		@NotNull(message = "L'heure de début est obligatoire.")
		LocalTime startTime,

		@NotNull(message = "L'heure de fin est obligatoire.")
		LocalTime endTime,

		@NotNull(message = "La date de début de validité est obligatoire.")
		LocalDate validFrom,

		LocalDate validTo
) {
}
