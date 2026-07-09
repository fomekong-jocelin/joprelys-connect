package com.joprelys.backend.emergency.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateEmergencyRequest(
		@NotNull(message = "L'identifiant du patient est obligatoire.")
		UUID patientId,

		@NotBlank(message = "Le mode d'arrivée est obligatoire.")
		@Size(max = 50)
		String arrivalMode,

		@NotBlank(message = "Le niveau de triage est obligatoire.")
		@Size(max = 20)
		String triageLevel,

		@NotBlank(message = "L'état hémodynamique est obligatoire.")
		@Size(max = 50)
		String hemodynamicStatus,

		@NotBlank(message = "Le motif est obligatoire.")
		String chiefComplaint,

		Integer initialBpSystolic,

		Integer initialBpDiastolic,

		Integer initialHr,

		BigDecimal initialTemp
) {}
