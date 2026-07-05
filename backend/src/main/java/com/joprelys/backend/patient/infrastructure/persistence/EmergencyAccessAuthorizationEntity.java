package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_access_authorizations")
public class EmergencyAccessAuthorizationEntity {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "doctor_email", nullable = false, length = 320)
	private String doctorEmail;

	@Column(name = "reason", nullable = false, columnDefinition = "TEXT")
	private String reason;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected EmergencyAccessAuthorizationEntity() {
	}

	public EmergencyAccessAuthorizationEntity(UUID patientId, UUID organizationId, String doctorEmail, String reason, Instant expiresAt) {
		this.id = UUID.randomUUID();
		this.patientId = patientId;
		this.organizationId = organizationId;
		this.doctorEmail = doctorEmail;
		this.reason = reason;
		this.expiresAt = expiresAt;
	}

	@PrePersist
	void prePersist() {
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getPatientId() {
		return patientId;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public String getDoctorEmail() {
		return doctorEmail;
	}

	public String getReason() {
		return reason;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setPatientId(java.util.UUID patientId) {
		this.patientId = patientId;
	}
}
