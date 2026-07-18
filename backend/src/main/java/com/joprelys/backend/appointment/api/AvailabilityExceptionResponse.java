package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityExceptionEntity;
import java.time.Instant;
import java.util.UUID;

/** Représentation API d'une indisponibilité ponctuelle (contrat §2). */
public record AvailabilityExceptionResponse(
		UUID id,
		UUID doctorId,
		Instant startAt,
		Instant endAt,
		String reason,
		Instant createdAt,
		Instant updatedAt
) {

	public static AvailabilityExceptionResponse fromEntity(DoctorAvailabilityExceptionEntity entity) {
		return new AvailabilityExceptionResponse(
				entity.getId(),
				entity.getDoctor().getId(),
				entity.getStartAt(),
				entity.getEndAt(),
				entity.getReason(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
