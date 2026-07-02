package com.joprelys.backend.audit.application;

import com.joprelys.backend.audit.infrastructure.persistence.AuditLogEntity;
import java.util.List;
import java.util.UUID;

public interface AuditService {

	void log(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
			 String resourceType, UUID resourceId, String action, String reason,
			 String ipAddress, String userAgent, String status);

	void logSuccess(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
					String resourceType, UUID resourceId, String action, String reason);

	void logDenied(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
				   String resourceType, UUID resourceId, String action, String reason);

	List<AuditLogEntity> getPatientLogs(UUID patientId);

	List<AuditLogEntity> getOrganizationLogs(UUID organizationId);
}
