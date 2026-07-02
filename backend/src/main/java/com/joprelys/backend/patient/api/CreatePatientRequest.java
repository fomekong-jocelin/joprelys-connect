package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePatientRequest(
		@NotBlank(message = "Le nom complet est obligatoire")
		@Size(min = 3, max = 255, message = "Le nom doit avoir entre 3 et 255 caractères")
		String fullName,

		@NotBlank(message = "Le sexe est obligatoire")
		String gender,

		@NotNull(message = "La date de naissance est obligatoire")
		@PastOrPresent(message = "La date de naissance ne peut pas être dans le futur")
		LocalDate birthDate,

		@NotBlank(message = "Le téléphone est obligatoire")
		String phone,

		@NotBlank(message = "La ville est obligatoire")
		String city,

		String district,
		String address,
		String emergencyContactName,
		String emergencyContactPhone,
		String allergies,
		String medicalHistory
) {
}
