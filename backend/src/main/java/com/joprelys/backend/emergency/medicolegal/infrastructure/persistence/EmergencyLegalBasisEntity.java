package com.joprelys.backend.emergency.medicolegal.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.medicolegal.domain.EmergencyLegalBasisType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_legal_bases")
public class EmergencyLegalBasisEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @Enumerated(EnumType.STRING)
    @Column(name = "basis_type", nullable = false, length = 48)
    private EmergencyLegalBasisType basisType;

    @Column(name = "justification", nullable = false, length = 1500)
    private String justification;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closure_reason", length = 500)
    private String closureReason;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "emergency_legal_basis_acts",
            joinColumns = @JoinColumn(name = "legal_basis_id"))
    @Column(name = "covered_act", nullable = false, length = 255)
    private Set<String> coveredActs = new HashSet<>();

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EmergencyLegalBasisEntity() {
    }

    public EmergencyLegalBasisEntity(
            UUID organizationId,
            EmergencyEntity emergency,
            EmergencyLegalBasisType basisType,
            String justification,
            Instant startsAt,
            Instant expiresAt,
            Set<String> coveredActs,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.emergency = emergency;
        this.basisType = basisType;
        this.justification = requireText(justification);
        this.startsAt = startsAt == null ? Instant.now() : startsAt;
        this.expiresAt = expiresAt;
        if (coveredActs != null) {
            coveredActs.stream()
                    .map(EmergencyLegalBasisEntity::normalize)
                    .filter(value -> value != null)
                    .forEach(this.coveredActs::add);
        }
        this.createdByUserId = createdByUserId;
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

    public void close(Instant closedAt, String reason) {
        if (this.closedAt != null) return;
        this.closedAt = closedAt == null ? Instant.now() : closedAt;
        this.closureReason = requireText(reason);
    }

    public boolean isActiveAt(Instant instant) {
        Instant reference = instant == null ? Instant.now() : instant;
        return !startsAt.isAfter(reference)
                && closedAt == null
                && (expiresAt == null || expiresAt.isAfter(reference));
    }

    private static String requireText(String value) {
        String normalized = normalize(value);
        if (normalized == null) throw new IllegalArgumentException("EMERGENCY_LEGAL_BASIS_JUSTIFICATION_REQUIRED");
        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public EmergencyLegalBasisType getBasisType() { return basisType; }
    public String getJustification() { return justification; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getClosedAt() { return closedAt; }
    public String getClosureReason() { return closureReason; }
    public Set<String> getCoveredActs() { return Set.copyOf(coveredActs); }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
