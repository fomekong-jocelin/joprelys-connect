package com.joprelys.backend.lab.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus;

public record LabResultUploadRequest(
		String examRequestNumber,
		String validatorName,
		UUID validatorUserId,
		LabResultStatus status,
		Instant sampleCollectedAt,
		Instant resultAt,
		Instant validatedAt,
		String conclusion,
		List<LabResultItem> results,
		String pdfBase64
) {}
