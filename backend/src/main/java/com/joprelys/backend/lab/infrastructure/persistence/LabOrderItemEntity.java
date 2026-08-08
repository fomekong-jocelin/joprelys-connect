package com.joprelys.backend.lab.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_order_items")
public class LabOrderItemEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lab_order_id", nullable = false)
	private LabOrderEntity labOrder;

	@Column(name = "exam_name", nullable = false)
	private String examName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private LabOrderStatus status = LabOrderStatus.REQUESTED;

	@Column(name = "sample_collected_at")
	private Instant sampleCollectedAt;

	@Column(name = "result_at")
	private Instant resultAt;

	@Column(name = "validated_at")
	private Instant validatedAt;

	protected LabOrderItemEntity() {
	}

	public LabOrderItemEntity(LabOrderEntity labOrder, String examName) {
		this.id = UUID.randomUUID();
		this.labOrder = labOrder;
		this.examName = examName;
		this.status = LabOrderStatus.REQUESTED;
	}

	public UUID getId() {
		return id;
	}

	public LabOrderEntity getLabOrder() {
		return labOrder;
	}

	public String getExamName() {
		return examName;
	}

	public LabOrderStatus getStatus() {
		return status;
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

	public void applyStatus(LabOrderStatus nextStatus, Instant now) {
		this.status = nextStatus;
		if ((nextStatus == LabOrderStatus.SAMPLE_COLLECTED
				|| nextStatus == LabOrderStatus.IN_PROGRESS
				|| nextStatus == LabOrderStatus.RESULT_AVAILABLE
				|| nextStatus == LabOrderStatus.VALIDATED)
				&& sampleCollectedAt == null) {
			this.sampleCollectedAt = now;
		}
		if ((nextStatus == LabOrderStatus.RESULT_AVAILABLE || nextStatus == LabOrderStatus.VALIDATED)
				&& resultAt == null) {
			this.resultAt = now;
		}
		if (nextStatus == LabOrderStatus.VALIDATED && validatedAt == null) {
			this.validatedAt = now;
		}
	}

	public void applyResultStatus(
			LabOrderStatus nextStatus,
			Instant collectedAt,
			Instant producedAt,
			Instant confirmedAt,
			Instant now) {
		this.status = nextStatus;
		if (sampleCollectedAt == null) {
			this.sampleCollectedAt = collectedAt != null ? collectedAt : now;
		}
		if (resultAt == null) {
			this.resultAt = producedAt != null ? producedAt : now;
		}
		if (nextStatus == LabOrderStatus.VALIDATED && validatedAt == null) {
			this.validatedAt = confirmedAt != null ? confirmedAt : now;
		}
	}
}
