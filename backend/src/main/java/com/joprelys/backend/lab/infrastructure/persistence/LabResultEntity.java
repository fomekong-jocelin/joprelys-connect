package com.joprelys.backend.lab.infrastructure.persistence;

import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
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
@Table(name = "lab_results")
public class LabResultEntity {

	@Id
	private UUID id;

	@Column(name = "result_number", nullable = false, unique = true, length = 50)
	private String resultNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lab_order_id", nullable = false)
	private LabOrderEntity labOrder;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@TenantId
	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "validator_name", nullable = false, length = 150)
	private String validatorName;

	@Column(name = "analyte_name", nullable = false, length = 100)
	private String analyteName;

	@Column(name = "result_value", nullable = false, length = 50)
	private String value;

	@Column(name = "unit", length = 20)
	private String unit;

	@Column(name = "reference_range", length = 50)
	private String referenceRange;

	@Column(name = "interpretation", nullable = false, length = 20)
	private String interpretation;

	@Column(name = "comment")
	private String comment;

	@Column(name = "pdf_file_path", length = 500)
	private String pdfFilePath;

	@Column(name = "sample_collected_at")
	private Instant sampleCollectedAt;

	@Column(name = "result_at")
	private Instant resultAt;

	@Column(name = "validated_at")
	private Instant validatedAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected LabResultEntity() {
	}

	public LabResultEntity(
			String resultNumber,
			LabOrderEntity labOrder,
			PatientEntity patient,
			String validatorName,
			String analyteName,
			String value,
			String unit,
			String referenceRange,
			String interpretation,
			String comment,
			String pdfFilePath,
			Instant sampleCollectedAt,
			Instant resultAt,
			Instant validatedAt) {
		this.id = UUID.randomUUID();
		this.resultNumber = resultNumber;
		this.labOrder = labOrder;
		this.patient = patient;
		this.organizationId = labOrder.getOrganizationId();
		this.validatorName = validatorName;
		this.analyteName = analyteName;
		this.value = value;
		this.unit = unit;
		this.referenceRange = referenceRange;
		this.interpretation = interpretation != null ? interpretation : "NORMAL";
		this.comment = comment;
		this.pdfFilePath = pdfFilePath;
		this.sampleCollectedAt = sampleCollectedAt;
		this.resultAt = resultAt;
		this.validatedAt = validatedAt;
	}

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getResultNumber() {
		return resultNumber;
	}

	public LabOrderEntity getLabOrder() {
		return labOrder;
	}

	public PatientEntity getPatient() {
		return patient;
	}

	public UUID getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(UUID organizationId) {
		this.organizationId = organizationId;
	}

	public String getValidatorName() {
		return validatorName;
	}

	public String getAnalyteName() {
		return analyteName;
	}

	public String getValue() {
		return value;
	}

	public String getUnit() {
		return unit;
	}

	public String getReferenceRange() {
		return referenceRange;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public String getComment() {
		return comment;
	}

	public String getPdfFilePath() {
		return pdfFilePath;
	}

	public Instant getSampleCollectedAt() {
		return sampleCollectedAt;
	}

	public Instant getResultAt() {
		return resultAt;
	}

	public Instant getValidatedAt() {
		return validatedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setPatient(PatientEntity patient) {
		this.patient = patient;
	}
}
