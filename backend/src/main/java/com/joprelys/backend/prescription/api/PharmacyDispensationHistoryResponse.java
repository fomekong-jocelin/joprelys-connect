package com.joprelys.backend.prescription.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PharmacyDispensationHistoryResponse(
		UUID dispensationId,
		Instant dispensedAt,
		String pharmacyName,
		String pharmacistLicense,
		List<PharmacyDispensationHistoryItemResponse> items
) {}
