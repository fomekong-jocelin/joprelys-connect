package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "coefficient", precision = 19, scale = 4)
    private BigDecimal coefficient;

    @Column(name = "total_item_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalItemAmount;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    protected InvoiceItemEntity() {
    }

    public InvoiceItemEntity(String label, InvoiceItemType itemType, BigDecimal unitPrice, BigDecimal quantity, BigDecimal coefficient) {
        this.id = UUID.randomUUID();
        this.label = label;
        this.itemType = itemType;
        this.unitPrice = unitPrice;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.coefficient = coefficient;
        recalculateTotal();
    }

    public void recalculateTotal() {
        BigDecimal mult = coefficient != null ? coefficient : BigDecimal.ONE;
        this.totalItemAmount = unitPrice.multiply(quantity).multiply(mult).setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public InvoiceItemType getItemType() { return itemType; }
    public void setItemType(InvoiceItemType itemType) { this.itemType = itemType; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; recalculateTotal(); }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; recalculateTotal(); }
    public BigDecimal getCoefficient() { return coefficient; }
    public void setCoefficient(BigDecimal coefficient) { this.coefficient = coefficient; recalculateTotal(); }
    public BigDecimal getTotalItemAmount() { return totalItemAmount; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
