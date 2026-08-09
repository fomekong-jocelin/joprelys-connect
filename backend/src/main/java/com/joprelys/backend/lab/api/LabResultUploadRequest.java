package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
		String pdfBase64,
		UUID labOrderItemId
) {
	public LabResultUploadRequest(
			String examRequestNumber,
			String validatorName,
			UUID validatorUserId,
			LabResultStatus status,
			Instant sampleCollectedAt,
			Instant resultAt,
			Instant validatedAt,
			String conclusion,
			List<LabResultItem> results,
			String pdfBase64) {
		this(
				examRequestNumber,
				validatorName,
				validatorUserId,
				status,
				sampleCollectedAt,
				resultAt,
				validatedAt,
				conclusion,
				results,
				pdfBase64,
				null);
	}
}
