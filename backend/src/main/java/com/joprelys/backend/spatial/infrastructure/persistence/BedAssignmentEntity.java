package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bed_assignments")
public class BedAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bed_id", nullable = false)
    private BedEntity bed;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    protected BedAssignmentEntity() {
    }

    public BedAssignmentEntity(UUID hospitalizationId, BedEntity bed) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.bed = bed;
        this.assignedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getHospitalizationId() {
        return hospitalizationId;
    }

    public void setHospitalizationId(UUID hospitalizationId) {
        this.hospitalizationId = hospitalizationId;
    }

    public BedEntity getBed() {
        return bed;
    }

    public void setBed(BedEntity bed) {
        this.bed = bed;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(Instant releasedAt) {
        this.releasedAt = releasedAt;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
