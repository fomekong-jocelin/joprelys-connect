package com.joprelys.backend.prescription.api;

import com.joprelys.backend.prescription.infrastructure.persistence.PrescriptionItemEntity;
import java.util.UUID;

public record PrescriptionItemResponse(
		UUID id,
		String drugName,
		String dosage,
		String posology,
		String duration,
		String quantity,
		String instructions,
		int sortOrder
) {
	public static PrescriptionItemResponse fromEntity(PrescriptionItemEntity e) {
		return new PrescriptionItemResponse(
				e.getId(),
				e.getDrugName(),
				e.getDosage(),
				e.getPosology(),
				e.getDuration(),
				e.getQuantity(),
				e.getInstructions(),
				e.getSortOrder()
		);
	}
}
