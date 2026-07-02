package com.joprelys.backend.lab.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateLabOrderRequest(
		@NotNull(message = "L'ID du patient est requis")
		UUID patientId,

		UUID visitId,

		UUID targetOrganizationId,

		@NotNull(message = "Le type d'examen est requis")
		String examType,

		@NotEmpty(message = "La liste des examens ne doit pas être vide")
		List<String> exams,

		String reason,

		String priority
) {}
