package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_consents")
public class PatientConsentEntity {

	@Id
	private UUID id;

	@Column(name = "patient_id", nullable = false)
	private UUID patientId;

	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	// WT1 (SCOPES): Granular access scopes
	@Column(name = "scopes", length = 500)
	private String scopes = "medical_records,prescriptions,lab_results,allergies_history";

	// WT1 (SCOPES): Validation channel (PORTAL, OTP_SMS, OTP_EMAIL)
	@Column(name = "validation_channel", length = 50)
	private String validationChannel = "PORTAL";

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PatientConsentEntity() {
	}

	public PatientConsentEntity(UUID patientId, UUID organizationId, String status) {
		this.id = UUID.randomUUID();
		this.patientId = patientId;
		this.organizationId = organizationId;
		this.status = status;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	// WT1 (SCOPES): Getters/Setters pour scopes granulaires
	public String getScopes() {
		return scopes;
	}

	public void setScopes(String scopes) {
		this.scopes = scopes;
	}

	// WT1 (SCOPES): Getters/Setters pour canal de validation
	public String getValidationChannel() {
		return validationChannel;
	}

	public void setValidationChannel(String validationChannel) {
		this.validationChannel = validationChannel;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	// WT3 (DUPLICATES): Setter for patient reassignment during merge
	public void setPatientId(UUID patientId) {
		this.patientId = patientId;
	}
}
