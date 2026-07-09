package com.joprelys.backend.emergency.api;

import com.joprelys.backend.emergency.infrastructure.persistence.ResuscitationLogEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResuscitationLogResponse(
		UUID id,
		UUID emergencyId,
		String actionType,
		String description,
		BigDecimal quantity,
		String unit,
		Instant administeredAt,
		UUID administeredByUserId
) {
	public static ResuscitationLogResponse fromEntity(ResuscitationLogEntity entity) {
		return new ResuscitationLogResponse(
				entity.getId(),
				entity.getEmergency().getId(),
				entity.getActionType(),
				entity.getDescription(),
				entity.getQuantity(),
				entity.getUnit(),
				entity.getAdministeredAt(),
				entity.getAdministeredByUserId()
		);
	}
}
