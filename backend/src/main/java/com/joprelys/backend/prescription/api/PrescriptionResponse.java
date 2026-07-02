package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
		UUID id,
		UUID consultationId,
		List<PrescriptionItemResponse> items,
		Instant createdAt,
		Instant updatedAt
) {
	public static PrescriptionResponse fromEntity(PrescriptionEntity e) {
		return new PrescriptionResponse(
				e.getId(),
				e.getConsultation().getId(),
				e.getItems().stream().map(PrescriptionItemResponse::fromEntity).toList(),
				e.getCreatedAt(),
				e.getUpdatedAt()
		);
	}
}
