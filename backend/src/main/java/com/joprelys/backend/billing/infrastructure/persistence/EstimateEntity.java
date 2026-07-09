package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "estimates")
public class EstimateEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "estimate_number", nullable = false, unique = true, length = 50)
    private String estimateNumber;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "patient_share", nullable = false)
    private Double patientShare;

    @Column(name = "insurance_share", nullable = false)
    private Double insuranceShare;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // DRAFT, ACCEPTED, REJECTED, INVOICED

    @OneToMany(mappedBy = "estimate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstimateItemEntity> items = new ArrayList<>();

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected EstimateEntity() {
    }

    public EstimateEntity(UUID patientId, UUID visitId, String estimateNumber) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.visitId = visitId;
        this.estimateNumber = estimateNumber;
        this.totalAmount = 0.0;
        this.patientShare = 0.0;
        this.insuranceShare = 0.0;
        this.status = "DRAFT";
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

    public void addItem(EstimateItemEntity item) {
        items.add(item);
        item.setEstimate(this);
        recalculateTotals();
    }

    public void recalculateTotals() {
        double total = 0.0;
        for (EstimateItemEntity item : items) {
            total += item.getUnitPrice() * item.getQuantity();
        }
        this.totalAmount = total;
        this.patientShare = total;
        this.insuranceShare = 0.0;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public String getEstimateNumber() {
        return estimateNumber;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Double getPatientShare() {
        return patientShare;
    }

    public void setPatientShare(Double patientShare) {
        this.patientShare = patientShare;
    }

    public Double getInsuranceShare() {
        return insuranceShare;
    }

    public void setInsuranceShare(Double insuranceShare) {
        this.insuranceShare = insuranceShare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<EstimateItemEntity> getItems() {
        return items;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
        for (EstimateItemEntity item : items) {
            item.setOrganizationId(organizationId);
        }
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
