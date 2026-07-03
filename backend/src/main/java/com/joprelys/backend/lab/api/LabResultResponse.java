package com.joprelys.backend.lab.api;

import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import java.time.Instant;
import java.util.UUID;

public record LabResultResponse(
		UUID id,
		String resultNumber,
		String examRequestNumber,
		UUID patientId,
		String validatorName,
		String analyteName,
		String value,
		String unit,
		String referenceRange,
		String interpretation,
		String comment,
		String pdfFilePath,
		Instant sampleCollectedAt,
		Instant resultAt,
		Instant validatedAt,
		Instant createdAt
) {
	public static LabResultResponse fromEntity(LabResultEntity entity) {
		return new LabResultResponse(
				entity.getId(),
				entity.getResultNumber(),
				entity.getLabOrder().getExamRequestNumber(),
				entity.getPatient().getId(),
				entity.getValidatorName(),
				entity.getAnalyteName(),
				entity.getValue(),
				entity.getUnit(),
				entity.getReferenceRange(),
				entity.getInterpretation(),
				entity.getComment(),
				entity.getPdfFilePath(),
				entity.getSampleCollectedAt(),
				entity.getResultAt(),
				entity.getValidatedAt(),
				entity.getCreatedAt()
		);
	}
}
