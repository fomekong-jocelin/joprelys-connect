package com.joprelys.backend.reception.api;

import com.joprelys.backend.reception.infrastructure.persistence.ReceptionLogEntity;
import java.time.Instant;
import java.util.UUID;

public record ReceptionLogResponse(
		UUID id,
		UUID organizationId,
		String logType,
		String firstName,
		String lastName,
		String idDocumentType,
		String idDocumentNumber,
		UUID targetPatientId,
		UUID targetStaffId,
		String reason,
		Instant arrivalAt,
		Instant departureAt,
		UUID createdByUserId,
		Instant createdAt,
		Instant updatedAt
) {
	public static ReceptionLogResponse fromEntity(ReceptionLogEntity entity) {
		return new ReceptionLogResponse(
				entity.getId(),
				entity.getOrganizationId(),
				entity.getLogType(),
				entity.getFirstName(),
				entity.getLastName(),
				entity.getIdDocumentType(),
				entity.getIdDocumentNumber(),
				entity.getTargetPatientId(),
				entity.getTargetStaffId(),
				entity.getReason(),
				entity.getArrivalAt(),
				entity.getDepartureAt(),
				entity.getCreatedByUserId(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
