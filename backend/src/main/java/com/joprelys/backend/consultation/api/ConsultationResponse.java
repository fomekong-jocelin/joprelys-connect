package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
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
		Instant updatedAt,
		UUID documentId,
		String documentStatus
) {
	public static ConsultationResponse fromEntity(ConsultationEntity entity) {
		return fromEntity(entity, null);
	}

	public static ConsultationResponse fromEntity(ConsultationEntity entity, MedicalDocumentEntity document) {
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
				entity.getUpdatedAt(),
				document != null ? document.getId() : null,
				document != null ? document.getStatus() : null
		);
	}
}
