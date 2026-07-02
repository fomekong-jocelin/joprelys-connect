package com.joprelys.backend.clinic.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClinicAdminRequest(
		@NotBlank(message = "Le nom complet est obligatoire.")
		@Size(max = 160, message = "Le nom complet ne doit pas dépasser 160 caractères.")
		String displayName,

		@NotBlank(message = "L'adresse e-mail est obligatoire.")
		@Email(message = "L'adresse e-mail est invalide.")
		@Size(max = 320, message = "L'adresse e-mail ne doit pas dépasser 320 caractères.")
		String email
) {
}
