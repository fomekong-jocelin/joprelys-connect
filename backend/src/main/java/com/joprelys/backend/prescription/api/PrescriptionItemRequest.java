package com.joprelys.backend.prescription.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionItemRequest(
		@NotBlank(message = "Le nom du médicament est obligatoire.")
		@Size(max = 200)
		String drugName,

		@NotBlank(message = "Le dosage est obligatoire.")
		@Size(max = 200)
		String dosage,

		@Size(max = 500)
		String posology,

		@Size(max = 100)
		String duration,

		@Size(max = 100)
		String quantity,

		@Size(max = 2000)
		String instructions
) {}
