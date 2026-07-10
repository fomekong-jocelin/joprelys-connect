package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "insurance_bordereaux")
public class InsuranceBordereauEntity {

    private static final int MONEY_SCALE = 4;

    @Id
    private UUID id;

    @Column(name = "bordereau_number", nullable = false, unique = true, length = 50)
    private String bordereauNumber;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "insurance_convention_id", nullable = false)
    private InsuranceConventionEntity insuranceConvention;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "accepted_amount", precision = 19, scale = 4)
    private BigDecimal acceptedAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount;

    @Column(name = "insurer_reference", length = 120)
    private String insurerReference;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private InsuranceBordereauStatus status;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected InsuranceBordereauEntity() {
    }

    public InsuranceBordereauEntity(String bordereauNumber,
                                     InsuranceConventionEntity insuranceConvention,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     BigDecimal totalAmount) {
        this.id = UUID.randomUUID();
        this.bordereauNumber = bordereauNumber;
        this.insuranceConvention = insuranceConvention;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = normalize(totalAmount);
        this.paidAmount = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        this.status = InsuranceBordereauStatus.DRAFT;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.totalAmount = normalize(this.totalAmount);
        this.paidAmount = normalize(this.paidAmount);
        this.acceptedAmount = normalizeNullable(this.acceptedAmount);
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
        this.totalAmount = normalize(this.totalAmount);
        this.paidAmount = normalize(this.paidAmount);
        this.acceptedAmount = normalizeNullable(this.acceptedAmount);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeNullable(BigDecimal value) {
        return value == null ? null : normalize(value);
    }

    public UUID getId() { return id; }
    public String getBordereauNumber() { return bordereauNumber; }
    public InsuranceConventionEntity getInsuranceConvention() { return insuranceConvention; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = normalize(totalAmount); }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public void setAcceptedAmount(BigDecimal acceptedAmount) { this.acceptedAmount = normalizeNullable(acceptedAmount); }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = normalize(paidAmount); }
    public BigDecimal getRemainingAmount() { return normalize(totalAmount.subtract(paidAmount)); }
    public BigDecimal getDisputedAmount() {
        return acceptedAmount == null ? BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                : normalize(totalAmount.subtract(acceptedAmount));
    }
    public String getInsurerReference() { return insurerReference; }
    public void setInsurerReference(String insurerReference) { this.insurerReference = insurerReference; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
    public InsuranceBordereauStatus getStatus() { return status; }
    public void setStatus(InsuranceBordereauStatus status) { this.status = status; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
