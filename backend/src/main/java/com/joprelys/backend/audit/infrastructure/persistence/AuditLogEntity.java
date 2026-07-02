package com.joprelys.backend.audit.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

	@Id
	private UUID id;

	@Column(name = "actor_user_id")
	private UUID actorUserId;

	@Column(name = "actor_organization_id")
	private UUID actorOrganizationId;

	@Column(name = "patient_id")
	private UUID patientId;

	@Column(name = "resource_type", length = 80)
	private String resourceType;

	@Column(name = "resource_id")
	private UUID resourceId;

	@Column(name = "action", nullable = false, length = 80)
	private String action;

	@Column(name = "reason")
	private String reason;

	@Column(name = "ip_address", length = 80)
	private String ipAddress;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public AuditLogEntity() {
	}

	public AuditLogEntity(UUID id, UUID actorUserId, UUID actorOrganizationId, UUID patientId,
						  String resourceType, UUID resourceId, String action, String reason,
						  String ipAddress, String userAgent, String status, Instant createdAt) {
		this.id = id;
		this.actorUserId = actorUserId;
		this.actorOrganizationId = actorOrganizationId;
		this.patientId = patientId;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.action = action;
		this.reason = reason;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
		this.status = status;
		this.createdAt = createdAt;
	}

	@PrePersist
	protected void onCreate() {
		if (this.id == null) {
			this.id = UUID.randomUUID();
		}
		if (this.createdAt == null) {
			this.createdAt = Instant.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getActorUserId() {
		return actorUserId;
	}

	public void setActorUserId(UUID actorUserId) {
		this.actorUserId = actorUserId;
	}

	public UUID getActorOrganizationId() {
		return actorOrganizationId;
	}

	public void setActorOrganizationId(UUID actorOrganizationId) {
		this.actorOrganizationId = actorOrganizationId;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public void setPatientId(UUID patientId) {
		this.patientId = patientId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public UUID getResourceId() {
		return resourceId;
	}

	public void setResourceId(UUID resourceId) {
		this.resourceId = resourceId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
