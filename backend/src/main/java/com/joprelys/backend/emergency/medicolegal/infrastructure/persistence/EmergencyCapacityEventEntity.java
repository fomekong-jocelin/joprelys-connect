package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyCapacityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_capacity_events")
public class EmergencyCapacityEventEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_status", nullable = false, length = 24)
    private EmergencyCapacityStatus capacityStatus;

    @Column(name = "consciousness_level", length = 32)
    private String consciousnessLevel;

    @Column(name = "clinical_reason", nullable = false, length = 1000)
    private String clinicalReason;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "recorded_by_user_id")
    private UUID recordedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmergencyCapacityEventEntity() {
    }

    public EmergencyCapacityEventEntity(
            UUID organizationId,
            EmergencyEntity emergency,
            EmergencyCapacityStatus capacityStatus,
            String consciousnessLevel,
            String clinicalReason,
            Instant effectiveAt,
            UUID recordedByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.emergency = emergency;
        this.capacityStatus = capacityStatus;
        this.consciousnessLevel = normalize(consciousnessLevel);
        this.clinicalReason = requireText(clinicalReason);
        this.effectiveAt = effectiveAt == null ? Instant.now() : effectiveAt;
        this.recordedByUserId = recordedByUserId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private static String requireText(String value) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException("EMERGENCY_CAPACITY_REASON_REQUIRED");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public EmergencyCapacityStatus getCapacityStatus() { return capacityStatus; }
    public String getConsciousnessLevel() { return consciousnessLevel; }
    public String getClinicalReason() { return clinicalReason; }
    public Instant getEffectiveAt() { return effectiveAt; }
    public UUID getRecordedByUserId() { return recordedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
