package com.joprelys.backend.prescription.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PharmacyVerifyResponse(
		UUID prescriptionId,
		String prescriptionNumber,
		String status,
		String patientName,
		String doctorName,
		Instant issuedAt,
		Instant expiresAt,
		boolean pharmaceuticalValidated,
        List<PharmacyVerifyItem> items
) {}
