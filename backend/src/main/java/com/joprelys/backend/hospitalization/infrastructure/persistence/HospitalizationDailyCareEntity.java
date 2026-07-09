package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitalization_daily_cares")
public class HospitalizationDailyCareEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "care_type", nullable = false, length = 100)
    private String careType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "billable", nullable = false)
    private boolean billable;

    @Column(name = "price")
    private Double price;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HospitalizationDailyCareEntity() {
    }

    public HospitalizationDailyCareEntity(UUID hospitalizationId, String careType, String description, boolean billable, Double price, String performedBy, Instant performedAt) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.careType = careType;
        this.description = description;
        this.billable = billable;
        this.price = price;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
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

    public UUID getId() { return id; }
    public UUID getHospitalizationId() { return hospitalizationId; }
    public UUID getOrganizationId() { return organizationId; }
    public Long getVersion() { return version; }
    public String getCareType() { return careType; }
    public String getDescription() { return description; }
    public boolean isBillable() { return billable; }
    public Double getPrice() { return price; }
    public String getPerformedBy() { return performedBy; }
    public Instant getPerformedAt() { return performedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
