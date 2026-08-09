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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lab_order_item_id")
	private LabOrderItemEntity labOrderItem;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "patient_id", nullable = false)
	private PatientEntity patient;

	@TenantId
	@Column(name = "organization_id", nullable = false)
	private UUID organizationId;

	@Column(name = "validator_name", length = 150)
	private String validatorName;

	@jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LabResultStatus status = LabResultStatus.VALIDATED;

	@Column(name = "validator_user_id")
	private UUID validatorUserId;

	@Column(name = "conclusion")
	private String conclusion;

	@Column(name = "document_id")
	private UUID documentId;

	@Column(name = "version", nullable = false)
	private int version = 1;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_result_id")
	private LabResultEntity parentResult;

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
		this(
				resultNumber,
				labOrder,
				null,
				patient,
				validatorName,
				LabResultStatus.VALIDATED,
				null,
				null,
				null,
				1,
				null,
				analyteName,
				value,
				unit,
				referenceRange,
				interpretation,
				comment,
				pdfFilePath,
				sampleCollectedAt,
				resultAt,
				validatedAt);
	}

	public LabResultEntity(
			String resultNumber,
			LabOrderEntity labOrder,
			PatientEntity patient,
			String validatorName,
			LabResultStatus status,
			UUID validatorUserId,
			String conclusion,
			UUID documentId,
			int version,
			LabResultEntity parentResult,
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
		this(
				resultNumber,
				labOrder,
				null,
				patient,
				validatorName,
				status,
				validatorUserId,
				conclusion,
				documentId,
				version,
				parentResult,
				analyteName,
				value,
				unit,
				referenceRange,
				interpretation,
				comment,
				pdfFilePath,
				sampleCollectedAt,
				resultAt,
				validatedAt);
	}

	public LabResultEntity(
			String resultNumber,
			LabOrderEntity labOrder,
			LabOrderItemEntity labOrderItem,
			PatientEntity patient,
			String validatorName,
			LabResultStatus status,
			UUID validatorUserId,
			String conclusion,
			UUID documentId,
			int version,
			LabResultEntity parentResult,
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
		this.labOrderItem = labOrderItem;
		this.patient = patient;
		this.organizationId = labOrder.getOrganizationId();
		this.validatorName = validatorName;
		this.status = status != null ? status : LabResultStatus.DRAFT;
		this.validatorUserId = validatorUserId;
		this.conclusion = conclusion;
		this.documentId = documentId;
		this.version = version;
		this.parentResult = parentResult;
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

	public LabOrderItemEntity getLabOrderItem() {
		return labOrderItem;
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

	public LabResultStatus getStatus() {
		return status;
	}

	public void setStatus(LabResultStatus status) {
		this.status = status;
	}

	public UUID getValidatorUserId() {
		return validatorUserId;
	}

	public void setValidatorUserId(UUID validatorUserId) {
		this.validatorUserId = validatorUserId;
	}

	public String getConclusion() {
		return conclusion;
	}

	public void setConclusion(String conclusion) {
		this.conclusion = conclusion;
	}

	public UUID getDocumentId() {
		return documentId;
	}

	public void setDocumentId(UUID documentId) {
		this.documentId = documentId;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public LabResultEntity getParentResult() {
		return parentResult;
	}

	public void setParentResult(LabResultEntity parentResult) {
		this.parentResult = parentResult;
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
