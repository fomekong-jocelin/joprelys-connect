package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
		UUID id,
		UUID consultationId,
		String prescriptionNumber,
		List<PrescriptionItemResponse> items,
		String pinCode,
		String status,
		Instant expiresAt,
		String transmissionStatus,
		Instant transmittedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static PrescriptionResponse fromEntity(PrescriptionEntity e) {
		return new PrescriptionResponse(
				e.getId(),
				e.getConsultation().getId(),
				e.getPrescriptionNumber(),
				e.getItems().stream().map(PrescriptionItemResponse::fromEntity).toList(),
				e.getPinCode(),
				e.getStatus(),
				e.getExpiresAt(),
				e.getTransmissionStatus(),
				e.getTransmittedAt(),
				e.getCreatedAt(),
				e.getUpdatedAt()
		);
	}
}
