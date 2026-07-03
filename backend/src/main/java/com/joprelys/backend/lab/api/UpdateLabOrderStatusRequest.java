package com.joprelys.backend.lab.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateLabOrderStatusRequest(
		@NotBlank(message = "Le statut est obligatoire.")
		String status
) {}
