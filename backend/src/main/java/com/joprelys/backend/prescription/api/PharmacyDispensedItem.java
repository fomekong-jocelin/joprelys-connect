package com.joprelys.backend.prescription.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PharmacyDispensedItem(
		@NotNull(message = "L'ID de la ligne d'ordonnance est obligatoire.")
		UUID prescriptionItemId,

		@NotNull(message = "La quantité dispensée est obligatoire.")
		Integer quantityDispensed,

		String substitutedWith
) {}
