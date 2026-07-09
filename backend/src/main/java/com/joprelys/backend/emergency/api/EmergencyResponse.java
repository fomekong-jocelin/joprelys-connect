package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmergencyResponse(
		UUID id,
		UUID organizationId,
		UUID patientId,
		String patientName,
		UUID visitId,
		String arrivalMode,
		String triageLevel,
		String hemodynamicStatus,
		String chiefComplaint,
		Integer initialBpSystolic,
		Integer initialBpDiastolic,
		Integer initialHr,
		BigDecimal initialTemp,
		Instant stabilizedAt,
		String orientation,
		UUID createdByUserId,
		List<ResuscitationLogResponse> resuscitationLogs,
		Instant createdAt,
		Instant updatedAt
) {
	public static EmergencyResponse fromEntity(EmergencyEntity entity) {
		var logs = entity.getResuscitationLogs() != null
				? entity.getResuscitationLogs().stream().map(ResuscitationLogResponse::fromEntity).toList()
				: List.<ResuscitationLogResponse>of();

		return new EmergencyResponse(
				entity.getId(),
				entity.getOrganizationId(),
				entity.getPatient().getId(),
				entity.getPatient().getFullName(),
				entity.getVisitId(),
				entity.getArrivalMode(),
				entity.getTriageLevel(),
				entity.getHemodynamicStatus(),
				entity.getChiefComplaint(),
				entity.getInitialBpSystolic(),
				entity.getInitialBpDiastolic(),
				entity.getInitialHr(),
				entity.getInitialTemp(),
				entity.getStabilizedAt(),
				entity.getOrientation(),
				entity.getCreatedByUserId(),
				logs,
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
