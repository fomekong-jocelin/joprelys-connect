package com.joprelys.backend.lab.infrastructure.persistence;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_orders")
public class LabOrderEntity {

	@Id
	private UUID id;

	@Column(name = "exam_request_number", nullable = false, unique = true, length = 50)
	private String examRequestNumber;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "visit_id")
	private VisitEntity visit;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "requester_practitioner_id", nullable = false)
	private UserAccountEntity requesterPractitioner;

	@Column(name = "target_organization_id")
	private UUID targetOrganizationId;

	@Column(name = "exam_type", nullable = false, length = 30)
	private String examType;

	@Column(name = "exams", nullable = false, columnDefinition = "TEXT")
	private String exams;

	@Column(name = "reason")
	private String reason;

	@Column(name = "priority", nullable = false, length = 20)
	private String priority;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected LabOrderEntity() {
	}

	public LabOrderEntity(
			String examRequestNumber,
			PatientEntity patient,
			VisitEntity visit,
			UserAccountEntity requesterPractitioner,
			UUID targetOrganizationId,
			String examType,
			String exams,
			String reason,
			String priority) {
		this.id = UUID.randomUUID();
		this.examRequestNumber = examRequestNumber;
		this.patient = patient;
		this.visit = visit;
		this.requesterPractitioner = requesterPractitioner;
		this.targetOrganizationId = targetOrganizationId;
		this.examType = examType;
		this.exams = exams;
		this.reason = reason;
		this.priority = priority != null ? priority : "NORMALE";
		this.status = "REQUESTED";
	}

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getExamRequestNumber() {
		return examRequestNumber;
	}

	public void setExamRequestNumber(String examRequestNumber) {
		this.examRequestNumber = examRequestNumber;
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

	public VisitEntity getVisit() {
		return visit;
	}

	public void setVisit(VisitEntity visit) {
		this.visit = visit;
	}

	public UserAccountEntity getRequesterPractitioner() {
		return requesterPractitioner;
	}

	public void setRequesterPractitioner(UserAccountEntity requesterPractitioner) {
		this.requesterPractitioner = requesterPractitioner;
	}

	public UUID getTargetOrganizationId() {
		return targetOrganizationId;
	}

	public void setTargetOrganizationId(UUID targetOrganizationId) {
		this.targetOrganizationId = targetOrganizationId;
	}

	public String getExamType() {
		return examType;
	}

	public void setExamType(String examType) {
		this.examType = examType;
	}

	public String getExams() {
		return exams;
	}

	public void setExams(String exams) {
		this.exams = exams;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
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
}
