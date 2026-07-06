package com.joprelys.backend.lab.infrastructure.persistence;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

	@Column(name = "source_organization_id")
	private UUID sourceOrganizationId;

	@Column(name = "exam_type", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private ExamType examType;

	@OneToMany(mappedBy = "labOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LabOrderItemEntity> items = new ArrayList<>();

	@Column(name = "reason")
	private String reason;

	@Column(name = "priority", nullable = false, length = 20)
	private String priority;

	@Column(name = "status", nullable = false, length = 30)
	@Enumerated(EnumType.STRING)
	private LabOrderStatus status;

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
			ExamType examType,
			List<String> exams,
			String reason,
			String priority,
			UUID sourceOrganizationId) {
		this.id = UUID.randomUUID();
		this.examRequestNumber = examRequestNumber;
		this.patient = patient;
		this.visit = visit;
		this.requesterPractitioner = requesterPractitioner;
		this.targetOrganizationId = targetOrganizationId;
		this.examType = examType;
		this.reason = reason;
		this.priority = priority != null ? priority : "NORMALE";
		this.status = LabOrderStatus.REQUESTED;
		this.sourceOrganizationId = sourceOrganizationId;
		if (exams != null) {
			for (String exam : exams) {
				this.items.add(new LabOrderItemEntity(this, exam));
			}
		}
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

	public UUID getSourceOrganizationId() {
		return sourceOrganizationId;
	}

	public void setSourceOrganizationId(UUID sourceOrganizationId) {
		this.sourceOrganizationId = sourceOrganizationId;
	}

	public ExamType getExamType() {
		return examType;
	}

	public void setExamType(ExamType examType) {
		this.examType = examType;
	}

	public List<LabOrderItemEntity> getItems() {
		return items;
	}

	public void setItems(List<LabOrderItemEntity> items) {
		this.items = items;
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

	public LabOrderStatus getStatus() {
		return status;
	}

	public void setStatus(LabOrderStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
