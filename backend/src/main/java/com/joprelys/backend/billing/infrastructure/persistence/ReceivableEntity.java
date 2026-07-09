package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "receivables")
public class ReceivableEntity {

    @Id
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "debtor_type", nullable = false, length = 50)
    private String debtorType; // PATIENT, INSURANCE

    @Column(name = "debtor_id", nullable = false)
    private UUID debtorId; // Patient ID or InsuranceConvention ID

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private Double paidAmount;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // UNPAID, PARTIALLY_PAID, PAID

    @Column(name = "due_date")
    private Instant dueDate;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected ReceivableEntity() {
    }

    public ReceivableEntity(UUID invoiceId, String debtorType, UUID debtorId, Double totalAmount) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.debtorType = debtorType;
        this.debtorId = debtorId;
        this.totalAmount = totalAmount;
        this.paidAmount = 0.0;
        this.status = "UNPAID";
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

    public String getDebtorType() {
        return debtorType;
    }

    public UUID getDebtorId() {
        return debtorId;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
        if (this.paidAmount >= this.totalAmount) {
            this.status = "PAID";
        } else if (this.paidAmount > 0) {
            this.status = "PARTIALLY_PAID";
        } else {
            this.status = "UNPAID";
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
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
}
