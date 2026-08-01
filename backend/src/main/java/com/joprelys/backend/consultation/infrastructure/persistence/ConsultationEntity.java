package com.joprelys.backend.consultation.infrastructure.persistence;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "consultations")
public class ConsultationEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "visit_id", nullable = false, unique = true)
	private VisitEntity visit;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "doctor_id", nullable = false)
	private UserAccountEntity doctor;

	@Column(name = "document_number", nullable = false, unique = true, length = 50)
	private String documentNumber;

	@Column(name = "symptoms", nullable = false, columnDefinition = "TEXT")
	private String symptoms;

	@Column(name = "clinical_exam", columnDefinition = "TEXT")
	private String clinicalExam;

	@Column(name = "diagnosis", nullable = false, columnDefinition = "TEXT")
	private String diagnosis;

	@Column(name = "conclusion", columnDefinition = "TEXT")
	private String conclusion;

	@Column(name = "advice", columnDefinition = "TEXT")
	private String advice;

	@Column(name = "follow_up", columnDefinition = "TEXT")
	private String followUp;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ConsultationEntity() {
	}

	public ConsultationEntity(
			VisitEntity visit,
			UserAccountEntity doctor,
			String documentNumber,
			String symptoms,
			String clinicalExam,
			String diagnosis,
			String advice,
			String followUp) {
		this(visit, doctor, documentNumber, symptoms, clinicalExam, diagnosis, null, advice, followUp);
	}

	public ConsultationEntity(
			VisitEntity visit,
			UserAccountEntity doctor,
			String documentNumber,
			String symptoms,
			String clinicalExam,
			String diagnosis,
			String conclusion,
			String advice,
			String followUp) {
		this.id = UUID.randomUUID();
		this.visit = visit;
		this.doctor = doctor;
		this.documentNumber = documentNumber;
		this.symptoms = symptoms;
		this.clinicalExam = clinicalExam;
		this.diagnosis = diagnosis;
		this.conclusion = conclusion;
		this.advice = advice;
		this.followUp = followUp;
		this.status = "BROUILLON";
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

	public UUID getId() { return id; }

	public UUID getOrganizationId() { return organizationId; }

	public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

	public VisitEntity getVisit() { return visit; }

	public UserAccountEntity getDoctor() { return doctor; }

	public void setDoctor(UserAccountEntity doctor) { this.doctor = doctor; }

	public String getDocumentNumber() { return documentNumber; }

	public String getSymptoms() { return symptoms; }

	public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

	public String getClinicalExam() { return clinicalExam; }

	public void setClinicalExam(String clinicalExam) { this.clinicalExam = clinicalExam; }

	public String getDiagnosis() { return diagnosis; }

	public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

	public String getConclusion() { return conclusion; }

	public void setConclusion(String conclusion) { this.conclusion = conclusion; }

	public String getAdvice() { return advice; }

	public void setAdvice(String advice) { this.advice = advice; }

	public String getFollowUp() { return followUp; }

	public void setFollowUp(String followUp) { this.followUp = followUp; }

	public String getStatus() { return status; }

	public void setStatus(String status) { this.status = status; }

	public Instant getCreatedAt() { return createdAt; }

	public Instant getUpdatedAt() { return updatedAt; }
}
