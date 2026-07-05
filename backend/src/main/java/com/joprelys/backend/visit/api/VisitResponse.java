package com.joprelys.backend.visit.api;

import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.time.Instant;
import java.util.UUID;

public record VisitResponse(
		UUID id,
		String visitNumber,
		UUID patientId,
		String patientName,
		String patientDpu,
		String reason,
		String orientation,
		String service,
		UUID mainPractitionerId,
		String status,
		Instant arrivalAt,
		Instant createdAt,
		Instant closedAt,
		VitalsResponse vitals
) {
	public static VisitResponse fromEntity(VisitEntity entity) {
		return new VisitResponse(
				entity.getId(),
				entity.getVisitNumber(),
				entity.getPatient().getId(),
				entity.getPatient().getFullName(),
				entity.getPatient().getGlobalPatientNumber(),
				entity.getReason(),
				entity.getOrientation(),
				entity.getService(),
				entity.getMainPractitionerId(),
				entity.getStatus(),
				entity.getArrivalAt(),
				entity.getCreatedAt(),
				entity.getClosedAt(),
				VitalsResponse.fromEntity(entity.getVitals())
		);
	}
}
