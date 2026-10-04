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
	private String status = "DRAFT";

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "transmission_status", nullable = false, length = 20)
	private String transmissionStatus = "NOT_TRANSMITTED";

	@Column(name = "transmitted_at")
	private Instant transmittedAt;

	@Column(name = "issued_at")
	private Instant issuedAt;

	@Column(name = "visit_id")
	private UUID visitId;

	@Column(name = "document_id")
	private UUID documentId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

    @jakarta.persistence.Column(name = "signed_by")
    private UUID signedBy;
    @jakarta.persistence.Column(name = "signed_at")
    private Instant signedAt;
    @jakarta.persistence.Column(name = "signed_content_hash", length = 64)
    private String signedContentHash;
    public UUID getSignedBy() { return signedBy; }
    public Instant getSignedAt() { return signedAt; }
    public String getSignedContentHash() { return signedContentHash; }
    public void seal(UUID actorId, Instant at, String hash) {
        if (signedAt != null) throw new IllegalStateException("Acte déjà signé.");
        signedBy = actorId;
        signedAt = at;
        signedContentHash = hash;
    }

	protected PrescriptionEntity() {}

	public PrescriptionEntity(ConsultationEntity consultation) {
		this.id = UUID.randomUUID();
		this.consultation = consultation;
		if (consultation != null && consultation.getVisit() != null) {
			this.visitId = consultation.getVisit().getId();
		}
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
	public String getTransmissionStatus() { return transmissionStatus; }
	public void setTransmissionStatus(String transmissionStatus) { this.transmissionStatus = transmissionStatus; }
	public Instant getTransmittedAt() { return transmittedAt; }
	public void setTransmittedAt(Instant transmittedAt) { this.transmittedAt = transmittedAt; }
	public Instant getIssuedAt() { return issuedAt; }
	public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
	public UUID getVisitId() { return visitId; }
	public void setVisitId(UUID visitId) { this.visitId = visitId; }
	public UUID getDocumentId() { return documentId; }
	public void setDocumentId(UUID documentId) { this.documentId = documentId; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
