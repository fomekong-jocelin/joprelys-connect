package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credit_notes")
public class CreditNoteEntity {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "credit_note_number", nullable = false, unique = true, length = 50)
    private String creditNoteNumber;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // PENDING_APPROVAL, ACTIVE, CANCELLED

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "refunded_amount", nullable = false)
    private Double refundedAmount = 0.0;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected CreditNoteEntity() {
    }

    public CreditNoteEntity(UUID invoiceId, String creditNoteNumber, Double amount, String reason) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.creditNoteNumber = creditNoteNumber;
        this.amount = amount;
        this.reason = reason;
        this.status = "ACTIVE";
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getCreditNoteNumber() {
        return creditNoteNumber;
    }

    public Double getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Long getVersion() {
        return version;
    }

    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String CANCELLED = "CANCELLED";

    public void markIssued(UUID issuedBy, boolean requiresApproval) {
        this.issuedBy = issuedBy;
        this.status = requiresApproval ? PENDING_APPROVAL : ACTIVE;
    }

    public void approve(UUID approvedBy, Instant approvedAt) {
        this.status = ACTIVE;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }

    public void reject(UUID rejectedBy, Instant rejectedAt) {
        this.status = CANCELLED;
        this.approvedBy = rejectedBy;
        this.approvedAt = rejectedAt;
    }

    public void recordRefund(double refundAmount) {
        this.refundedAmount = (refundedAmount == null ? 0.0 : refundedAmount) + refundAmount;
    }

    public UUID getIssuedBy() { return issuedBy; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public Double getRefundedAmount() { return refundedAmount == null ? 0.0 : refundedAmount; }
}
