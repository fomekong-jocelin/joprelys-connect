package com.joprelys.backend.reception.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateReceptionLogRequest(
		@NotBlank(message = "Le type de log est obligatoire.")
		@Size(max = 50)
		String logType,

		@NotBlank(message = "Le prénom est obligatoire.")
		@Size(max = 100)
		String firstName,

		@NotBlank(message = "Le nom est obligatoire.")
		@Size(max = 100)
		String lastName,

		@Size(max = 50)
		String idDocumentType,

		@Size(max = 100)
		String idDocumentNumber,

		UUID targetPatientId,

		UUID targetStaffId,

		String reason,

		Instant arrivalAt
) {}
