package com.joprelys.backend.audit.application;

import com.joprelys.backend.audit.infrastructure.persistence.AuditLogEntity;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Écriture du journal d'audit.
 *
 * <p>Chaque écriture s'exécute dans sa propre transaction ({@code REQUIRES_NEW}) : un accès
 * en lecture ({@code readOnly = true}, flush manuel, connexion en lecture seule) ou une
 * transaction métier annulée ne doit jamais faire disparaître la trace de l'accès (SEC-04).</p>
 */
@Service
public class AuditServiceImpl implements AuditService {

	private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
	private final AuditLogRepository auditLogRepository;

	public AuditServiceImpl(AuditLogRepository auditLogRepository) {
		this.auditLogRepository = auditLogRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void logSuccess(UUID actorUserId, UUID actorOrganizationId, UUID patientId,
						   String resourceType, UUID resourceId, String action, String reason) {
		log(actorUserId, actorOrganizationId, patientId, resourceType, resourceId, action, reason, null, null, "SUCCESS");
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
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

	/**
	 * Adresse IP du client telle que résolue par le conteneur. Derrière un proxy de confiance,
	 * {@code server.forward-headers-strategy: native} fait déjà valoir l'IP réelle ; un en-tête
	 * {@code X-Forwarded-For} fourni par le client n'est jamais lu directement (SEC-05).
	 */
	private static String resolveIpAddress() {
		HttpServletRequest request = currentRequest();
		return request != null ? request.getRemoteAddr() : null;
	}

	private static String resolveUserAgent() {
		HttpServletRequest request = currentRequest();
		return request != null ? request.getHeader("User-Agent") : null;
	}

	private static HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttrs) {
			return servletAttrs.getRequest();
		}
		return null;
	}
}
