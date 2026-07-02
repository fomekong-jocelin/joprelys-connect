package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import java.time.Instant;
import java.util.UUID;

public record ConsultationResponse(
		UUID id,
		UUID visitId,
		String visitNumber,
		UUID doctorId,
		String doctorName,
		String documentNumber,
		String symptoms,
		String clinicalExam,
		String diagnosis,
		String advice,
		String followUp,
		String status,
		Instant createdAt,
		Instant updatedAt
) {
	public static ConsultationResponse fromEntity(ConsultationEntity entity) {
		return new ConsultationResponse(
				entity.getId(),
				entity.getVisit().getId(),
				entity.getVisit().getVisitNumber(),
				entity.getDoctor().getId(),
				entity.getDoctor().getDisplayName(),
				entity.getDocumentNumber(),
				entity.getSymptoms(),
				entity.getClinicalExam(),
				entity.getDiagnosis(),
				entity.getAdvice(),
				entity.getFollowUp(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
