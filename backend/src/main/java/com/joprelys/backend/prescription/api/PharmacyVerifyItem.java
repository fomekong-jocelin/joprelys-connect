package com.joprelys.backend.prescription.api;

import java.util.UUID;

/** Ligne d'ordonnance présentée au pharmacien : valeurs prescrites réelles, jamais de valeur par défaut inventée. */
public record PharmacyVerifyItem(
		UUID itemId,
		String drugName,
		String dosage,
		String form,
		String quantity,
		int quantityAlreadyDispensed,
		boolean substitutionAllowed,
		String instructions,
		String posology,
		String route,
		String frequency,
		String duration
) {}
