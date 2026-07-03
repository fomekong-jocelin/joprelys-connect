package com.joprelys.backend.prescription.infrastructure.persistence;

import com.joprelys.backend.consultation.infrastructure.persistence.ConsultationEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
public class PrescriptionEntity {

	@Id
	private UUID id;

	@TenantId
	@Column(name = "organization_id")
	private UUID organizationId;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "consultation_id", nullable = false, unique = true)
	private ConsultationEntity consultation;

	@OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("sortOrder ASC")
	private List<PrescriptionItemEntity> items = new ArrayList<>();

	@Column(name = "prescription_number", length = 50, unique = true)
	private String prescriptionNumber;

	@Column(name = "pin_code", length = 4)
	private String pinCode;

	@Column(name = "status", nullable = false, length = 20)
	private String status = "ACTIVE";

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PrescriptionEntity() {}

	public PrescriptionEntity(ConsultationEntity consultation) {
		this.id = UUID.randomUUID();
		this.consultation = consultation;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() { updatedAt = Instant.now(); }

	public UUID getId() { return id; }
	public UUID getOrganizationId() { return organizationId; }
	public ConsultationEntity getConsultation() { return consultation; }
	public List<PrescriptionItemEntity> getItems() { return items; }
	public String getPinCode() { return pinCode; }
	public void setPinCode(String pinCode) { this.pinCode = pinCode; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public Instant getExpiresAt() { return expiresAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
	public String getPrescriptionNumber() { return prescriptionNumber; }
	public void setPrescriptionNumber(String prescriptionNumber) { this.prescriptionNumber = prescriptionNumber; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
