package com.joprelys.backend.visit.infrastructure.persistence;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visits")
public class VisitEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@Column(name = "visit_number", nullable = false, unique = true, length = 50)
	private String visitNumber;

	@Column(name = "reason", nullable = false, columnDefinition = "TEXT")
	private String reason;

	@Column(name = "orientation", nullable = false, length = 100)
	private String orientation;

	@Column(name = "service_name", length = 100)
	private String service;

	@Column(name = "main_practitioner_id")
	private UUID mainPractitionerId;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "arrival_at")
	private Instant arrivalAt;

	@Column(name = "closed_at")
	private Instant closedAt;

	@jakarta.persistence.OneToOne(mappedBy = "visit", cascade = jakarta.persistence.CascadeType.ALL, fetch = FetchType.LAZY)
	private VitalsEntity vitals;

	protected VisitEntity() {
	}

	public VisitEntity(
			PatientEntity patient,
			String visitNumber,
			String reason,
			String orientation) {
		this(patient, visitNumber, reason, orientation, orientation, null, null);
	}

	public VisitEntity(
			PatientEntity patient,
			String visitNumber,
			String reason,
			String orientation,
			String service,
			UUID mainPractitionerId,
			Instant arrivalAt) {
		this.id = UUID.randomUUID();
		this.patient = patient;
		this.visitNumber = visitNumber;
		this.reason = reason;
		this.orientation = orientation;
		this.service = service;
		this.mainPractitionerId = mainPractitionerId;
		this.arrivalAt = arrivalAt;
		this.status = "EN_COURS";
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

	public PatientEntity getPatient() {
		return patient;
	}

	public void setPatient(PatientEntity patient) {
		this.patient = patient;
	}

	public String getVisitNumber() {
		return visitNumber;
	}

	public void setVisitNumber(String visitNumber) {
		this.visitNumber = visitNumber;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public UUID getMainPractitionerId() {
		return mainPractitionerId;
	}

	public void setMainPractitionerId(UUID mainPractitionerId) {
		this.mainPractitionerId = mainPractitionerId;
	}

	public Instant getArrivalAt() {
		return arrivalAt;
	}

	public void setArrivalAt(Instant arrivalAt) {
		this.arrivalAt = arrivalAt;
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

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Instant getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(Instant closedAt) {
		this.closedAt = closedAt;
	}

	public VitalsEntity getVitals() {
		return vitals;
	}

	public void setVitals(VitalsEntity vitals) {
		this.vitals = vitals;
	}
}
