package com.joprelys.backend.prescription.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescription_dispensations")
public class PrescriptionDispensationEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "prescription_id", nullable = false)
	private PrescriptionEntity prescription;

	@Column(name = "dispensed_at", nullable = false)
	private Instant dispensedAt;

	@Column(name = "pharmacy_name", nullable = false, length = 200)
	private String pharmacyName;

	@Column(name = "pharmacist_license", nullable = false, length = 50)
	private String pharmacistLicense;

	@OneToMany(mappedBy = "dispensation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<DispensationItemEntity> items = new ArrayList<>();

	@Column(name = "created_at")
	private Instant createdAt;

	protected PrescriptionDispensationEntity() {}

	public PrescriptionDispensationEntity(PrescriptionEntity prescription, String pharmacyName, String pharmacistLicense) {
		this.id = UUID.randomUUID();
		this.prescription = prescription;
		this.pharmacyName = pharmacyName;
		this.pharmacistLicense = pharmacistLicense;
		this.dispensedAt = Instant.now();
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public UUID getId() { return id; }
	public PrescriptionEntity getPrescription() { return prescription; }
	public Instant getDispensedAt() { return dispensedAt; }
	public String getPharmacyName() { return pharmacyName; }
	public String getPharmacistLicense() { return pharmacistLicense; }
	public List<DispensationItemEntity> getItems() { return items; }
	public Instant getCreatedAt() { return createdAt; }
}
