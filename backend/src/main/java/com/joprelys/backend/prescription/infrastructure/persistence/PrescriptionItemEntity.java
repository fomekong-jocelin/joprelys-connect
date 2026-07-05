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

	@Column(name = "form", length = 100)
	private String form;

	@Column(name = "route", length = 100)
	private String route;

	@Column(name = "frequency", length = 100)
	private String frequency;

	@Column(name = "substitution_allowed", nullable = false)
	private boolean substitutionAllowed = true;

	protected PrescriptionItemEntity() {}

	public PrescriptionItemEntity(PrescriptionEntity prescription, String drugName, String dosage,
			String posology, String duration, String quantity, String instructions, int sortOrder,
			String form, String route, String frequency, Boolean substitutionAllowed) {
		this.id = UUID.randomUUID();
		this.prescription = prescription;
		this.drugName = drugName;
		this.dosage = dosage;
		this.posology = posology;
		this.duration = duration;
		this.quantity = quantity;
		this.instructions = instructions;
		this.sortOrder = sortOrder;
		this.form = form;
		this.route = route;
		this.frequency = frequency;
		this.substitutionAllowed = substitutionAllowed != null ? substitutionAllowed : true;
	}

	public PrescriptionItemEntity(PrescriptionEntity prescription, String drugName, String dosage,
			String posology, String duration, String quantity, String instructions, int sortOrder) {
		this(prescription, drugName, dosage, posology, duration, quantity, instructions, sortOrder, null, null, null, true);
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
	public String getForm() { return form; }
	public String getRoute() { return route; }
	public String getFrequency() { return frequency; }
	public boolean isSubstitutionAllowed() { return substitutionAllowed; }
}
