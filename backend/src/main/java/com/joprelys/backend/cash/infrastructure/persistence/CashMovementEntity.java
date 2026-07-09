package com.joprelys.backend.cash.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_movements")
public class CashMovementEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_session_id", nullable = false)
    private CashRegisterSessionEntity cashRegisterSession;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType; // IN, OUT, TRANSFER_TO_BANK

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "description", nullable = false, length = 250)
    private String description;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // CASH, CHECK, BANK_TRANSFER

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashMovementEntity() {
    }

    public CashMovementEntity(CashRegisterSessionEntity cashRegisterSession, String movementType, Double amount, String description, String paymentMethod, String referenceNumber, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.cashRegisterSession = cashRegisterSession;
        this.movementType = movementType;
        this.amount = amount;
        this.description = description;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CashRegisterSessionEntity getCashRegisterSession() {
        return cashRegisterSession;
    }

    public void setCashRegisterSession(CashRegisterSessionEntity cashRegisterSession) {
        this.cashRegisterSession = cashRegisterSession;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
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
}
