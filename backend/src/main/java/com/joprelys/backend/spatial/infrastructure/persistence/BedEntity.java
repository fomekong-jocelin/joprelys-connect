package com.joprelys.backend.spatial.infrastructure.persistence;

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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "beds")
public class BedEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @Column(name = "bed_number", nullable = false, length = 20)
    private String bedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BedStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "capacity_status", nullable = false, length = 20)
    private BedCapacityStatus capacityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "readiness_status", nullable = false, length = 20)
    private BedReadinessStatus readinessStatus;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BedEntity() {
    }

    public BedEntity(RoomEntity room, String bedNumber) {
        this.id = UUID.randomUUID();
        this.room = room;
        this.bedNumber = bedNumber;
        this.capacityStatus = BedCapacityStatus.OPEN;
        this.readinessStatus = BedReadinessStatus.READY;
        this.status = BedStatus.FREE;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (capacityStatus == null) {
            capacityStatus = BedCapacityStatus.OPEN;
        }
        if (readinessStatus == null) {
            readinessStatus = readinessFromLegacyStatus(status);
        }
        synchronizeLegacyStatus();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public void setRoom(RoomEntity room) {
        this.room = room;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = Objects.requireNonNull(status, "Le statut legacy du lit est obligatoire.");
        switch (status) {
            case FREE -> {
                this.capacityStatus = BedCapacityStatus.OPEN;
                this.readinessStatus = BedReadinessStatus.READY;
            }
            case OCCUPIED -> {
                this.capacityStatus = BedCapacityStatus.OPEN;
                this.readinessStatus = BedReadinessStatus.READY;
            }
            case CLEANING -> this.readinessStatus = BedReadinessStatus.CLEANING;
            case MAINTENANCE -> this.readinessStatus = BedReadinessStatus.MAINTENANCE;
        }
    }

    public BedCapacityStatus getCapacityStatus() {
        return capacityStatus;
    }

    public void setCapacityStatus(BedCapacityStatus capacityStatus) {
        this.capacityStatus = Objects.requireNonNull(capacityStatus, "L'état de capacité du lit est obligatoire.");
        synchronizeLegacyStatus();
    }

    public BedReadinessStatus getReadinessStatus() {
        return readinessStatus;
    }

    public boolean isOpen() {
        return capacityStatus == BedCapacityStatus.OPEN;
    }

    public boolean isReady() {
        return readinessStatus == BedReadinessStatus.READY;
    }

    public boolean isOperationallyAvailable(boolean hasActiveAssignment) {
        return isOpen()
                && isReady()
                && !hasActiveAssignment
                && status == BedStatus.FREE;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void synchronizeLegacyStatus() {
        if (status == BedStatus.OCCUPIED) {
            return;
        }
        if (capacityStatus == BedCapacityStatus.CLOSED && readinessStatus == BedReadinessStatus.READY) {
            status = BedStatus.MAINTENANCE;
            return;
        }
        status = switch (readinessStatus) {
            case READY -> BedStatus.FREE;
            case CLEANING -> BedStatus.CLEANING;
            case MAINTENANCE -> BedStatus.MAINTENANCE;
        };
    }

    private BedReadinessStatus readinessFromLegacyStatus(BedStatus legacyStatus) {
        if (legacyStatus == null) {
            return BedReadinessStatus.READY;
        }
        return switch (legacyStatus) {
            case CLEANING -> BedReadinessStatus.CLEANING;
            case MAINTENANCE -> BedReadinessStatus.MAINTENANCE;
            case FREE, OCCUPIED -> BedReadinessStatus.READY;
        };
    }
}
