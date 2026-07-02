package com.joprelys.backend.auth.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_audit_events")
public class AuthAuditEventEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private Instant occurredAt;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(nullable = false, length = 64)
	private String ipAddress;

	@Column(nullable = false)
	private boolean success;

	@Column(length = 80)
	private String failureReason;

	protected AuthAuditEventEntity() {
	}

	public AuthAuditEventEntity(Instant occurredAt, String email, String ipAddress, boolean success, String failureReason) {
		this.id = UUID.randomUUID();
		this.occurredAt = occurredAt;
		this.email = email;
		this.ipAddress = ipAddress;
		this.success = success;
		this.failureReason = failureReason;
	}
}
