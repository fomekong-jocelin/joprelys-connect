package com.joprelys.backend.prescription.api;

import java.util.UUID;

public record PharmacyVerifyItem(
		UUID itemId,
		String drugName,
		String dosage,
		String form,
		String quantity,
		int quantityAlreadyDispensed,
		boolean substitutionAllowed,
		String instructions
) {}
