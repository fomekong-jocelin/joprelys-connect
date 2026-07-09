package com.joprelys.backend.billing.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;

@Entity
@Table(name = "estimate_items")
public class EstimateItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false)
    @JsonIgnore
    private EstimateEntity estimate;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "item_type", nullable = false, length = 50)
    private String itemType;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    protected EstimateItemEntity() {
    }

    public EstimateItemEntity(String label, String itemType, Double unitPrice, Double quantity) {
        this.id = UUID.randomUUID();
        this.label = label;
        this.itemType = itemType;
        this.unitPrice = unitPrice;
        this.quantity = quantity != null ? quantity : 1.0;
    }

    public UUID getId() {
        return id;
    }

    public EstimateEntity getEstimate() {
        return estimate;
    }

    public void setEstimate(EstimateEntity estimate) {
        this.estimate = estimate;
    }

    public String getLabel() {
        return label;
    }

    public String getItemType() {
        return itemType;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public Double getQuantity() {
        return quantity;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
