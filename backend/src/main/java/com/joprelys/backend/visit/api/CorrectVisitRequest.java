package com.joprelys.backend.visit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CorrectVisitRequest(
		@NotBlank(message = "Le motif de la correction est obligatoire.")
		String correctionReason,

		String reason,
		String orientation,
		String service,
		UUID mainPractitionerId,
		Instant arrivalAt
) {
}
