package com.joprelys.backend.prescription.infrastructure.persistence;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "dispensation_items")
public class DispensationItemEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "dispensation_id", nullable = false)
	private PrescriptionDispensationEntity dispensation;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prescription_item_id", nullable = false)
	private PrescriptionItemEntity prescriptionItem;

	@Column(name = "quantity_dispensed", nullable = false)
	private Integer quantityDispensed;

	@Column(name = "substituted_with", length = 200)
	private String substitutedWith;

	protected DispensationItemEntity() {}

	public DispensationItemEntity(PrescriptionDispensationEntity dispensation, PrescriptionItemEntity prescriptionItem, Integer quantityDispensed, String substitutedWith) {
		this.id = UUID.randomUUID();
		this.dispensation = dispensation;
		this.prescriptionItem = prescriptionItem;
		this.quantityDispensed = quantityDispensed;
		this.substitutedWith = substitutedWith;
	}

	public UUID getId() { return id; }
	public PrescriptionDispensationEntity getDispensation() { return dispensation; }
	public PrescriptionItemEntity getPrescriptionItem() { return prescriptionItem; }
	public Integer getQuantityDispensed() { return quantityDispensed; }
	public String getSubstitutedWith() { return substitutedWith; }
}
