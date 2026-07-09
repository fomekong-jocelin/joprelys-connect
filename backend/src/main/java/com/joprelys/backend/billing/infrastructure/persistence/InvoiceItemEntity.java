package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "label", nullable = false, length = 250)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private InvoiceItemType itemType;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "coefficient")
    private Double coefficient;

    @Column(name = "total_item_amount", nullable = false)
    private Double totalItemAmount;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    protected InvoiceItemEntity() {
    }

    public InvoiceItemEntity(String label, InvoiceItemType itemType, Double unitPrice, Double quantity, Double coefficient) {
        this.id = UUID.randomUUID();
        this.label = label;
        this.itemType = itemType;
        this.unitPrice = unitPrice;
        this.quantity = quantity != null ? quantity : 1.0;
        this.coefficient = coefficient;
        recalculateTotal();
    }

    public void recalculateTotal() {
        double mult = coefficient != null ? coefficient : 1.0;
        this.totalItemAmount = unitPrice * quantity * mult;
    }

    public UUID getId() {
        return id;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public InvoiceItemType getItemType() {
        return itemType;
    }

    public void setItemType(InvoiceItemType itemType) {
        this.itemType = itemType;
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
        recalculateTotal();
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
        recalculateTotal();
    }

    public Double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(Double coefficient) {
        this.coefficient = coefficient;
        recalculateTotal();
    }

    public Double getTotalItemAmount() {
        return totalItemAmount;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
