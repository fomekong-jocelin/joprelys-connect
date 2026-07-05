package com.joprelys.backend.visit.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

// STORY-0603 — Révocation et annulation de documents médicaux

@Entity
@Table(name = "medical_documents")
public class MedicalDocumentEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private VisitEntity visit;

    @Column(name = "document_number", nullable = false, unique = true, length = 100)
    private String documentNumber;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType = "SYNTHESE";

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // STORY-0603 — champs de traçabilité de révocation
    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by_user_id")
    private UUID revokedByUserId;

    @Column(name = "revocation_reason", length = 500)
    private String revocationReason;

    protected MedicalDocumentEntity() {
    }

    public MedicalDocumentEntity(VisitEntity visit, String documentNumber, String filePath) {
        this.id = UUID.randomUUID();
        this.visit = visit;
        this.documentNumber = documentNumber;
        this.filePath = filePath;
        this.status = "VALID";
        this.documentType = "SYNTHESE";
        if (visit != null) {
            this.organizationId = visit.getOrganizationId();
        }
    }

    public MedicalDocumentEntity(VisitEntity visit, String documentNumber, String filePath, String documentType) {
        this(visit, documentNumber, filePath);
        if (documentType != null) {
            this.documentType = documentType;
        }
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public VisitEntity getVisit() {
        return visit;
    }

    public void setVisit(VisitEntity visit) {
        this.visit = visit;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // STORY-0603 — méthode métier de révocation
    public void revoke(UUID revokedByUserId, String reason, String newStatus) {
        this.status = newStatus;
        this.revokedAt = Instant.now();
        this.revokedByUserId = revokedByUserId;
        this.revocationReason = reason;
        this.updatedAt = Instant.now();
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getRevokedByUserId() {
        return revokedByUserId;
    }

    public String getRevocationReason() {
        return revocationReason;
    }
}
