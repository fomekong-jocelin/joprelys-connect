package com.joprelys.backend.emergency.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record AddResuscitationLogRequest(
		@NotBlank(message = "Le type d'acte est obligatoire.")
		@Size(max = 50)
		String actionType,

		@NotBlank(message = "La description est obligatoire.")
		@Size(max = 255)
		String description,

		BigDecimal quantity,

		@Size(max = 20)
		String unit,

		Instant administeredAt
) {}
