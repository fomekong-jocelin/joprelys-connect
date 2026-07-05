package com.joprelys.backend.visit.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateVisitRequest(
		@NotNull(message = "L'identifiant du patient est obligatoire.")
		UUID patientId,

		@NotBlank(message = "Le motif de la visite est obligatoire.")
		String reason,

		@NotBlank(message = "Le service ou médecin d'orientation est obligatoire.")
		String orientation,

		String service,

		UUID mainPractitionerId,

		java.time.Instant arrivalAt
) {
}
