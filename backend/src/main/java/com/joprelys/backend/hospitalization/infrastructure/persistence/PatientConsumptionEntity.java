package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_consumptions")
public class PatientConsumptionEntity {

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

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    @Column(name = "consumed_by", length = 100)
    private String consumedBy;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientConsumptionEntity() {
    }

    public PatientConsumptionEntity(UUID hospitalizationId, String itemName, Integer quantity, double unitPrice, String consumedBy, Instant consumedAt) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.consumedBy = consumedBy;
        this.consumedAt = consumedAt;
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
    public String getItemName() { return itemName; }
    public Integer getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public String getConsumedBy() { return consumedBy; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
