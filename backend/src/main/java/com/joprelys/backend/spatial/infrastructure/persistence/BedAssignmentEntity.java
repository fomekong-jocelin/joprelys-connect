package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.Objects;
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

    @Column(name = "active_bed_id")
    private UUID activeBedId;

    @Column(name = "active_hospitalization_id")
    private UUID activeHospitalizationId;

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
        this.activeBedId = bed.getId();
        this.activeHospitalizationId = hospitalizationId;
    }

    @PrePersist
    @PreUpdate
    void validateAndSynchronizeActiveMarkers() {
        validatePeriod();
        synchronizeActiveMarkers();
    }

    private void synchronizeActiveMarkers() {
        boolean active = releasedAt == null;
        this.activeBedId = active && bed != null ? bed.getId() : null;
        this.activeHospitalizationId = active ? hospitalizationId : null;
    }

    private void validatePeriod() {
        if (assignedAt == null) {
            throw new IllegalStateException("La date de début de l'affectation est obligatoire.");
        }
        if (releasedAt != null && releasedAt.isBefore(assignedAt)) {
            throw new IllegalArgumentException(
                    "La date de fin de l'affectation ne peut pas précéder sa date de début.");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getHospitalizationId() {
        return hospitalizationId;
    }

    public void setHospitalizationId(UUID hospitalizationId) {
        this.hospitalizationId = hospitalizationId;
        synchronizeActiveMarkers();
    }

    public BedEntity getBed() {
        return bed;
    }

    public void setBed(BedEntity bed) {
        this.bed = bed;
        synchronizeActiveMarkers();
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = Objects.requireNonNull(
                assignedAt,
                "La date de début de l'affectation est obligatoire.");
        validatePeriod();
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public void releaseAt(Instant releasedAt) {
        this.releasedAt = Objects.requireNonNull(
                releasedAt,
                "La date de fin de l'affectation est obligatoire.");
        validatePeriod();
        synchronizeActiveMarkers();
    }

    public UUID getActiveBedId() {
        return activeBedId;
    }

    public UUID getActiveHospitalizationId() {
        return activeHospitalizationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }
}
