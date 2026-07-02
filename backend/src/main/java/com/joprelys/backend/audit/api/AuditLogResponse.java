package com.joprelys.backend.audit.api;

import com.joprelys.backend.audit.infrastructure.persistence.AuditLogEntity;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
		UUID id,
		UUID actorUserId,
		String actorName,
		UUID actorOrganizationId,
		UUID patientId,
		String resourceType,
		UUID resourceId,
		String action,
		String reason,
		String ipAddress,
		String userAgent,
		String status,
		Instant createdAt
) {
	public static AuditLogResponse fromEntity(AuditLogEntity entity, String actorName) {
		return new AuditLogResponse(
				entity.getId(),
				entity.getActorUserId(),
				actorName,
				entity.getActorOrganizationId(),
				entity.getPatientId(),
				entity.getResourceType(),
				entity.getResourceId(),
				entity.getAction(),
				entity.getReason(),
				entity.getIpAddress(),
				entity.getUserAgent(),
				entity.getStatus(),
				entity.getCreatedAt()
		);
	}
}
