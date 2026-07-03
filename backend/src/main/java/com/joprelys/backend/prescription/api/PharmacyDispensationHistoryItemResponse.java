package com.joprelys.backend.prescription.api;

import java.util.UUID;

public record PharmacyDispensationHistoryItemResponse(
		UUID prescriptionItemId,
		String drugName,
		Integer quantityDispensed,
		String substitutedWith
) {}
