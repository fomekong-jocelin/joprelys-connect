package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "insurance_bordereaux")
public class InsuranceBordereauEntity {

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

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

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

    public InsuranceBordereauEntity(String bordereauNumber, InsuranceConventionEntity insuranceConvention, LocalDate startDate, LocalDate endDate, Double totalAmount) {
        this.id = UUID.randomUUID();
        this.bordereauNumber = bordereauNumber;
        this.insuranceConvention = insuranceConvention;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalAmount = totalAmount;
        this.status = InsuranceBordereauStatus.DRAFT;
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

    public String getBordereauNumber() {
        return bordereauNumber;
    }

    public InsuranceConventionEntity getInsuranceConvention() {
        return insuranceConvention;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public InsuranceBordereauStatus getStatus() {
        return status;
    }

    public void setStatus(InsuranceBordereauStatus status) {
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
}
