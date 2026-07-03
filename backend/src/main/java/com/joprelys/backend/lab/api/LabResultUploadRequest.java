package com.joprelys.backend.lab.api;

import java.time.Instant;
import java.util.List;

public record LabResultUploadRequest(
		String examRequestNumber,
		String validatorName,
		Instant sampleCollectedAt,
		Instant resultAt,
		Instant validatedAt,
		String conclusion,
		List<LabResultItem> results,
		String pdfBase64
) {}
