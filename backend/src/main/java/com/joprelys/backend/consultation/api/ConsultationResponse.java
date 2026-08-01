package com.joprelys.backend.consultation.api;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import com.joprelys.backend.prescription.api.PrescriptionItemResponse;
import com.joprelys.backend.visit.api.VitalsResponse;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import java.time.Instant;
import java.util.List;
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
		String conclusion,
		String advice,
		String followUp,
		String status,
		Instant createdAt,
		Instant updatedAt,
		UUID documentId,
		String documentStatus,
		VitalsResponse vitals,
		List<PrescriptionItemResponse> prescriptionItems,
		UUID prescriptionId,
		String prescriptionNumber,
		String prescriptionStatus,
		String prescriptionTransmissionStatus,
		Instant prescriptionTransmittedAt,
		UUID prescriptionDocumentId,
		String pinCode
) {
	public static ConsultationResponse fromEntity(ConsultationEntity entity) {
		return fromEntity(entity, null, null, List.of(), null);
	}

	public static ConsultationResponse fromEntity(ConsultationEntity entity, MedicalDocumentEntity document) {
		return fromEntity(entity, document, null, List.of(), null);
	}

	public static ConsultationResponse fromEntity(
			ConsultationEntity entity,
			MedicalDocumentEntity document,
			VitalsResponse vitals,
			List<PrescriptionItemResponse> prescriptionItems) {
		return fromEntity(entity, document, vitals, prescriptionItems, null);
	}

	public static ConsultationResponse fromEntity(
			ConsultationEntity entity,
			MedicalDocumentEntity document,
			VitalsResponse vitals,
			List<PrescriptionItemResponse> prescriptionItems,
			com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity prescription) {
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
				entity.getConclusion(),
				entity.getAdvice(),
				entity.getFollowUp(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				document != null ? document.getId() : null,
				document != null ? document.getStatus() != null ? document.getStatus().name() : null : null,
				vitals,
				prescriptionItems,
				prescription != null ? prescription.getId() : null,
				prescription != null ? prescription.getPrescriptionNumber() : null,
				prescription != null ? prescription.getStatus() : null,
				prescription != null ? prescription.getTransmissionStatus() : null,
				prescription != null ? prescription.getTransmittedAt() : null,
				prescription != null ? prescription.getDocumentId() : null,
				prescription != null ? prescription.getPinCode() : null
		);
	}
}
