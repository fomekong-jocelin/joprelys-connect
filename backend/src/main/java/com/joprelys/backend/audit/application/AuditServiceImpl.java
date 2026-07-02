package com.joprelys.backend.audit.application;

import com.joprelys.backend.audit.infrastructure.persistence.AuditLogEntity;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuditServiceImpl implements AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
	private final AuditLogRepository auditLogRepository;

	public AuditServiceImpl(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Override
	public void log(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
					String resourceType, UUID resourceId, String action, String reason,
					String ipAddress, String userAgent, String status) {
		
		String resolvedIp = ipAddress != null ? ipAddress : resolveIpAddress();
		String resolvedUserAgent = userAgent != null ? userAgent : resolveUserAgent();

		var entry = new AuditLogEntity(
				UUID.randomUUID(),
				actorUserId,
				actorOrganizationId,
				patientId,
				resourceType,
				resourceId,
				action,
				reason,
				resolvedIp,
				resolvedUserAgent,
				status,
				Instant.now()
		);

		auditLogRepository.save(entry);
		log.info("AUDIT LOG: actor={} action={} patient={} status={}", actorUserId, action, patientId, status);
	}

	@Override
	public void logSuccess(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
						   String resourceType, UUID resourceId, String action, String reason) {
		log(actorUserId, actorOrganizationId, patientId, resourceType, resourceId, action, reason, null, null, "SUCCESS");
	}

	@Override
	public void logDenied(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
						  String resourceType, UUID resourceId, String action, String reason) {
		log(actorUserId, actorOrganizationId, patientId, resourceType, resourceId, action, reason, null, null, "DENIED");
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLogEntity> getPatientLogs(UUID patientId) {
		return auditLogRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AuditLogEntity> getOrganizationLogs(UUID organizationId) {
		return auditLogRepository.findByActorOrganizationIdOrderByCreatedAtDesc(organizationId);
	}

	private String resolveIpAddress() {
		try {
			var attrs = RequestContextHolder.getRequestAttributes();
			if (attrs instanceof ServletRequestAttributes servletAttrs) {
				var request = servletAttrs.getRequest();
				String xForwarded = request.getHeader("X-Forwarded-For");
				if (xForwarded != null && !xForwarded.isBlank()) {
					return xForwarded.split(",")[0].trim();
				}
				return request.getRemoteAddr();
			}
		} catch (Exception e) {
			// request context not available
		}
		return null;
	}

	private String resolveUserAgent() {
		try {
			var attrs = RequestContextHolder.getRequestAttributes();
			if (attrs instanceof ServletRequestAttributes servletAttrs) {
				return servletAttrs.getRequest().getHeader("User-Agent");
			}
		} catch (Exception e) {
			// request context not available
		}
		return null;
	}
}
