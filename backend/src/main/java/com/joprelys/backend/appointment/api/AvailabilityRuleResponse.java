package com.joprelys.backend.appointment.api;

import com.joprelys.backend.appointment.infrastructure.persistence.DoctorAvailabilityEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Représentation API d'une règle de disponibilité récurrente (contrat §2). */
public record AvailabilityRuleResponse(
		UUID id,
		UUID doctorId,
		Integer weekday,
		LocalTime startTime,
		LocalTime endTime,
		LocalDate validFrom,
		LocalDate validTo,
		Boolean active,
		Instant createdAt,
		Instant updatedAt
) {

	public static AvailabilityRuleResponse fromEntity(DoctorAvailabilityEntity entity) {
		return new AvailabilityRuleResponse(
				entity.getId(),
				entity.getDoctor().getId(),
				entity.getWeekday(),
				entity.getStartTime(),
				entity.getEndTime(),
				entity.getValidFrom(),
				entity.getValidTo(),
				entity.getActive(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
