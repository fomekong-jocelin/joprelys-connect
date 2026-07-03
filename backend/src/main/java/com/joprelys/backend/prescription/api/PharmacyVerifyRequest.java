package com.joprelys.backend.prescription.api;

import jakarta.validation.constraints.NotBlank;

public record PharmacyVerifyRequest(
		@NotBlank(message = "Le numéro d'ordonnance est obligatoire.")
		String prescriptionNumber,

		@NotBlank(message = "Le code PIN est obligatoire.")
		String pinCode
) {}
