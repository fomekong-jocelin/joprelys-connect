package com.joprelys.backend.prescription.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItemEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prescription_id", nullable = false)
	private PrescriptionEntity prescription;

	@Column(name = "drug_name", nullable = false, length = 200)
	private String drugName;

	@Column(name = "dosage", nullable = false, length = 200)
	private String dosage;

	@Column(name = "posology", length = 500)
	private String posology;

	@Column(name = "duration", length = 100)
	private String duration;

	@Column(name = "quantity", length = 100)
	private String quantity;

	@Column(name = "instructions", columnDefinition = "TEXT")
	private String instructions;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected PrescriptionItemEntity() {}

	public PrescriptionItemEntity(PrescriptionEntity prescription, String drugName, String dosage,
			String posology, String duration, String quantity, String instructions, int sortOrder) {
		this.id = UUID.randomUUID();
		this.prescription = prescription;
		this.drugName = drugName;
		this.dosage = dosage;
		this.posology = posology;
		this.duration = duration;
		this.quantity = quantity;
		this.instructions = instructions;
		this.sortOrder = sortOrder;
	}

	public UUID getId() { return id; }
	public PrescriptionEntity getPrescription() { return prescription; }
	public String getDrugName() { return drugName; }
	public String getDosage() { return dosage; }
	public String getPosology() { return posology; }
	public String getDuration() { return duration; }
	public String getQuantity() { return quantity; }
	public String getInstructions() { return instructions; }
	public int getSortOrder() { return sortOrder; }
}
