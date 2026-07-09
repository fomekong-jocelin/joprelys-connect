package com.joprelys.backend.reception.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reception_logs")
public class ReceptionLogEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "log_type", nullable = false, length = 50)
	private String logType;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "id_document_type", length = 50)
	private String idDocumentType;

	@Column(name = "id_document_number", length = 100)
	private String idDocumentNumber;

	@Column(name = "target_patient_id")
	private UUID targetPatientId;

	@Column(name = "target_staff_id")
	private UUID targetStaffId;

	@Column(name = "reason", columnDefinition = "TEXT")
	private String reason;

	@Column(name = "arrival_at", nullable = false)
	private Instant arrivalAt;

	@Column(name = "departure_at")
	private Instant departureAt;

	@Column(name = "created_by_user_id")
	private UUID createdByUserId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ReceptionLogEntity() {
	}

	public ReceptionLogEntity(
			String logType,
			String firstName,
			String lastName,
			String idDocumentType,
			String idDocumentNumber,
			UUID targetPatientId,
			UUID targetStaffId,
			String reason,
			Instant arrivalAt,
			UUID createdByUserId) {
		this.id = UUID.randomUUID();
		this.logType = logType;
		this.firstName = firstName;
		this.lastName = lastName;
		this.idDocumentType = idDocumentType;
		this.idDocumentNumber = idDocumentNumber;
		this.targetPatientId = targetPatientId;
		this.targetStaffId = targetStaffId;
		this.reason = reason;
		this.arrivalAt = arrivalAt != null ? arrivalAt : Instant.now();
		this.createdByUserId = createdByUserId;
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

	public UUID getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(UUID organizationId) {
		this.organizationId = organizationId;
	}

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getIdDocumentType() {
		return idDocumentType;
	}

	public void setIdDocumentType(String idDocumentType) {
		this.idDocumentType = idDocumentType;
	}

	public String getIdDocumentNumber() {
		return idDocumentNumber;
	}

	public void setIdDocumentNumber(String idDocumentNumber) {
		this.idDocumentNumber = idDocumentNumber;
	}

	public UUID getTargetPatientId() {
		return targetPatientId;
	}

	public void setTargetPatientId(UUID targetPatientId) {
		this.targetPatientId = targetPatientId;
	}

	public UUID getTargetStaffId() {
		return targetStaffId;
	}

	public void setTargetStaffId(UUID targetStaffId) {
		this.targetStaffId = targetStaffId;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Instant getArrivalAt() {
		return arrivalAt;
	}

	public void setArrivalAt(Instant arrivalAt) {
		this.arrivalAt = arrivalAt;
	}

	public Instant getDepartureAt() {
		return departureAt;
	}

	public void setDepartureAt(Instant departureAt) {
		this.departureAt = departureAt;
	}

	public UUID getCreatedByUserId() {
		return createdByUserId;
	}

	public void setCreatedByUserId(UUID createdByUserId) {
		this.createdByUserId = createdByUserId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
