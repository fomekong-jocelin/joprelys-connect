package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Acompte encaissé en caisse avant facturation, imputé ensuite sur une ou plusieurs factures (ADR-20261017 D-16). */
@Entity
@Table(name = "patient_deposits")
public class PatientDepositEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "deposit_number", nullable = false, unique = true, length = 50)
    private String depositNumber;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "cash_session_id", nullable = false)
    private UUID cashSessionId;

    @Column(name = "received_by_user_id", nullable = false)
    private UUID receivedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected PatientDepositEntity() {
    }

    public PatientDepositEntity(String depositNumber, UUID patientId, UUID visitId, BigDecimal amount,
                                PaymentMethod paymentMethod, String referenceNumber, String note,
                                UUID cashSessionId, UUID receivedByUserId) {
        this.id = UUID.randomUUID();
        this.depositNumber = depositNumber;
        this.patientId = patientId;
        this.visitId = visitId;
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
        this.remainingAmount = this.amount;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.note = note;
        this.cashSessionId = cashSessionId;
        this.receivedByUserId = receivedByUserId;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void consume(BigDecimal consumed) {
        if (consumed.signum() <= 0 || consumed.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Montant d'imputation invalide pour l'acompte " + depositNumber);
        }
        remainingAmount = remainingAmount.subtract(consumed).setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public String getDepositNumber() { return depositNumber; }
    public UUID getPatientId() { return patientId; }
    public UUID getVisitId() { return visitId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getNote() { return note; }
    public UUID getCashSessionId() { return cashSessionId; }
    public UUID getReceivedByUserId() { return receivedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
