package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyBelongingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_belongings")
public class EmergencyBelongingEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @Column(name = "category", nullable = false, length = 48)
    private String category;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "item_condition", length = 255)
    private String itemCondition;

    @Column(name = "seal_number", length = 80)
    private String sealNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "custody_status", nullable = false, length = 32)
    private EmergencyBelongingStatus custodyStatus;

    @Column(name = "deposited_by_name", length = 160)
    private String depositedByName;

    @Column(name = "received_by_user_id")
    private UUID receivedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EmergencyBelongingEntity() {
    }

    public EmergencyBelongingEntity(
            UUID organizationId,
            EmergencyEntity emergency,
            String category,
            String description,
            int quantity,
            String itemCondition,
            String sealNumber,
            String depositedByName,
            UUID receivedByUserId) {
        if (quantity <= 0) throw new IllegalArgumentException("EMERGENCY_BELONGING_QUANTITY_INVALID");
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.emergency = emergency;
        this.category = requireText(category, "EMERGENCY_BELONGING_CATEGORY_REQUIRED");
        this.description = requireText(description, "EMERGENCY_BELONGING_DESCRIPTION_REQUIRED");
        this.quantity = quantity;
        this.itemCondition = normalize(itemCondition);
        this.sealNumber = normalize(sealNumber);
        this.custodyStatus = EmergencyBelongingStatus.IN_CUSTODY;
        this.depositedByName = normalize(depositedByName);
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

    public void applyTransferStatus(EmergencyBelongingStatus targetStatus) {
        if (custodyStatus == EmergencyBelongingStatus.RELEASED
                || custodyStatus == EmergencyBelongingStatus.DISPOSED) {
            throw new IllegalStateException("EMERGENCY_BELONGING_CUSTODY_CLOSED");
        }
        custodyStatus = targetStatus;
    }

    private static String requireText(String value, String errorCode) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException(errorCode);
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public String getItemCondition() { return itemCondition; }
    public String getSealNumber() { return sealNumber; }
    public EmergencyBelongingStatus getCustodyStatus() { return custodyStatus; }
    public String getDepositedByName() { return depositedByName; }
    public UUID getReceivedByUserId() { return receivedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
