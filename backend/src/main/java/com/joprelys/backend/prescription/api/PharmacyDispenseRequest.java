package com.joprelys.backend.prescription.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PharmacyDispenseRequest(
		@NotBlank(message = "Le numéro d'ordonnance est obligatoire.")
		String prescriptionNumber,

		@NotBlank(message = "Le code PIN est obligatoire.")
		String pinCode,

		@NotBlank(message = "Le nom de la pharmacie est obligatoire.")
		@jakarta.validation.constraints.Size(max = 200, message = "Le nom de la pharmacie ne doit pas dépasser 200 caractères.")
		String pharmacyName,

		@NotBlank(message = "Le numéro de licence du pharmacien est obligatoire.")
		@jakarta.validation.constraints.Size(max = 50, message = "Le numéro de licence ne doit pas dépasser 50 caractères.")
		String pharmacistLicense,

		@NotEmpty(message = "La liste des médicaments délivrés ne peut pas être vide.")
		List<PharmacyDispensedItem> dispensedItems
) {}
