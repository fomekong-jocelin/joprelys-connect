package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "organizational_unit_space_assignments")
public class OrganizationalUnitSpaceAssignmentEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "organizational_unit_id", nullable = false)
    private UUID organizationalUnitId;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationalUnitSpaceAssignmentEntity() {
    }

    public OrganizationalUnitSpaceAssignmentEntity(
            UUID organizationId,
            UUID organizationalUnitId,
            UUID spaceId,
            Instant validFrom,
            Instant validTo) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.organizationalUnitId = organizationalUnitId;
        this.spaceId = spaceId;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public void updatePeriod(Instant validFrom, Instant validTo) {
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public boolean isActiveAt(Instant instant) {
        return !validFrom.isAfter(instant) && (validTo == null || validTo.isAfter(instant));
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getOrganizationalUnitId() { return organizationalUnitId; }
    public UUID getSpaceId() { return spaceId; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
