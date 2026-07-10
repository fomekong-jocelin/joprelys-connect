package com.joprelys.backend.cash.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cash_register_sessions")
public class CashRegisterSessionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegisterEntity cashRegister;

    @Column(name = "opened_by_user_id", nullable = false)
    private UUID openedByUserId;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "opening_balance", nullable = false)
    private Double openingBalance;

    @Column(name = "closed_by_user_id")
    private UUID closedByUserId;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closing_balance")
    private Double closingBalance;

    @Column(name = "declared_balance")
    private Double declaredBalance;

    @Column(name = "discrepancy_amount")
    private Double discrepancyAmount;

    @Column(name = "discrepancy_reason", length = 250)
    private String discrepancyReason;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // OPEN, CLOSED

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "discrepancy_resolved", nullable = false)
    private Boolean discrepancyResolved = false;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolved_by_user_id", length = 36)
    private String resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected CashRegisterSessionEntity() {
    }

    public CashRegisterSessionEntity(CashRegisterEntity cashRegister, UUID openedByUserId, Double openingBalance) {
        this.id = UUID.randomUUID();
        this.cashRegister = cashRegister;
        this.openedByUserId = openedByUserId;
        this.openingBalance = openingBalance;
        this.openedAt = Instant.now();
        this.status = "OPEN";
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

    public CashRegisterEntity getCashRegister() {
        return cashRegister;
    }

    public void setCashRegister(CashRegisterEntity cashRegister) {
        this.cashRegister = cashRegister;
    }

    public UUID getOpenedByUserId() {
        return openedByUserId;
    }

    public void setOpenedByUserId(UUID openedByUserId) {
        this.openedByUserId = openedByUserId;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Double getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(Double openingBalance) {
        this.openingBalance = openingBalance;
    }

    public UUID getClosedByUserId() {
        return closedByUserId;
    }

    public void setClosedByUserId(UUID closedByUserId) {
        this.closedByUserId = closedByUserId;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Double getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(Double closingBalance) {
        this.closingBalance = closingBalance;
    }

    public Double getDeclaredBalance() {
        return declaredBalance;
    }

    public void setDeclaredBalance(Double declaredBalance) {
        this.declaredBalance = declaredBalance;
    }

    public Double getDiscrepancyAmount() {
        return discrepancyAmount;
    }

    public void setDiscrepancyAmount(Double discrepancyAmount) {
        this.discrepancyAmount = discrepancyAmount;
    }

    public String getDiscrepancyReason() {
        return discrepancyReason;
    }

    public void setDiscrepancyReason(String discrepancyReason) {
        this.discrepancyReason = discrepancyReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDiscrepancyResolved() {
        return discrepancyResolved;
    }

    public void setDiscrepancyResolved(Boolean discrepancyResolved) {
        this.discrepancyResolved = discrepancyResolved;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvedByUserId() {
        return resolvedByUserId;
    }

    public void setResolvedByUserId(String resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
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
