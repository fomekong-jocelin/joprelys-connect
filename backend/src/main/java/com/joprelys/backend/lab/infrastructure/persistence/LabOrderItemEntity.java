package com.joprelys.backend.lab.infrastructure.persistence;

import jakarta.persistence.*;
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

	protected LabOrderItemEntity() {
	}

	public LabOrderItemEntity(LabOrderEntity labOrder, String examName) {
		this.id = UUID.randomUUID();
		this.labOrder = labOrder;
		this.examName = examName;
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
}
