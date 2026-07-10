package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
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
    private String debtorType;

    @Column(name = "debtor_id", nullable = false)
    private UUID debtorId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

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

    public ReceivableEntity(UUID invoiceId, String debtorType, UUID debtorId, BigDecimal totalAmount) {
        this.id = UUID.randomUUID();
        this.invoiceId = invoiceId;
        this.debtorType = debtorType;
        this.debtorId = debtorId;
        this.totalAmount = totalAmount;
        this.paidAmount = BigDecimal.ZERO;
        this.status = "UNPAID";
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getInvoiceId() { return invoiceId; }
    public String getDebtorType() { return debtorType; }
    public UUID getDebtorId() { return debtorId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
        int cmp = this.paidAmount.compareTo(this.totalAmount);
        if (cmp >= 0) {
            this.status = "PAID";
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = "PARTIALLY_PAID";
        } else {
            this.status = "UNPAID";
        }
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant dueDate) { this.dueDate = dueDate; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
