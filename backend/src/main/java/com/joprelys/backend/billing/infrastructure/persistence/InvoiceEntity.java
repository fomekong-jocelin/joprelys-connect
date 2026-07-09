package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "insurance_convention_id")
    private InsuranceConventionEntity insuranceConvention;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "patient_share", nullable = false)
    private Double patientShare;

    @Column(name = "insurance_share", nullable = false)
    private Double insuranceShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvoiceStatus status;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItemEntity> items = new ArrayList<>();

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "validated_by_user_id")
    private UUID validatedByUserId;

    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Column(name = "discount_reason")
    private String discountReason;

    @Version
    private Long version;

    protected InvoiceEntity() {
    }

    public InvoiceEntity(UUID patientId, UUID visitId, String invoiceNumber, InsuranceConventionEntity insuranceConvention) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.visitId = visitId;
        this.invoiceNumber = invoiceNumber;
        this.insuranceConvention = insuranceConvention;
        this.totalAmount = 0.0;
        this.patientShare = 0.0;
        this.insuranceShare = 0.0;
        this.status = InvoiceStatus.PENDING;
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

    public void addItem(InvoiceItemEntity item) {
        items.add(item);
        item.setInvoice(this);
        recalculateTotals();
    }

    public void removeItem(InvoiceItemEntity item) {
        items.remove(item);
        item.setInvoice(null);
        recalculateTotals();
    }

    public void recalculateTotals() {
        double total = 0.0;
        for (InvoiceItemEntity item : items) {
            total += item.getTotalItemAmount();
        }
        double discount = this.discountAmount != null ? this.discountAmount : 0.0;
        double netTotal = Math.max(0.0, total - discount);
        this.totalAmount = netTotal;

        if (insuranceConvention != null) {
            double coveragePct = insuranceConvention.getCoveragePercentage();
            this.insuranceShare = netTotal * coveragePct;
            this.patientShare = netTotal - this.insuranceShare;
        } else {
            this.insuranceShare = 0.0;
            this.patientShare = netTotal;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public InsuranceConventionEntity getInsuranceConvention() {
        return insuranceConvention;
    }

    public void setInsuranceConvention(InsuranceConventionEntity insuranceConvention) {
        this.insuranceConvention = insuranceConvention;
        recalculateTotals();
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Double getPatientShare() {
        return patientShare;
    }

    public Double getInsuranceShare() {
        return insuranceShare;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public List<InvoiceItemEntity> getItems() {
        return items;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        for (InvoiceItemEntity item : items) {
            item.setOrganizationId(organizationId);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public UUID getValidatedByUserId() {
        return validatedByUserId;
    }

    public void setValidatedByUserId(UUID validatedByUserId) {
        this.validatedByUserId = validatedByUserId;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
        recalculateTotals();
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public Long getVersion() {
        return version;
    }
}
