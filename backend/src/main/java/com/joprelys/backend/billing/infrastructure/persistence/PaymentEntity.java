package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "received_by_user_id", nullable = false)
    private UUID receivedByUserId;

    @Column(name = "cash_session_id")
    private UUID cashSessionId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentEntity() {
    }

    public PaymentEntity(InvoiceEntity invoice, BigDecimal amount, PaymentMethod paymentMethod, String referenceNumber, UUID receivedByUserId) {
        this.id = UUID.randomUUID();
        this.invoice = invoice;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.receivedByUserId = receivedByUserId;
    }

    @PrePersist
    void prePersist() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public UUID getReceivedByUserId() { return receivedByUserId; }
    public void setReceivedByUserId(UUID receivedByUserId) { this.receivedByUserId = receivedByUserId; }
    public UUID getCashSessionId() { return cashSessionId; }
    public void setCashSessionId(UUID cashSessionId) { this.cashSessionId = cashSessionId; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Instant getCreatedAt() { return createdAt; }
}
