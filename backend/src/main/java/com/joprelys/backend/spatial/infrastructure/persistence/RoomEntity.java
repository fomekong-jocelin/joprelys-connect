package com.joprelys.backend.spatial.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ward_id", nullable = false)
    private WardEntity ward;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "comfort_level", nullable = false, length = 50)
    private String comfortLevel;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RoomEntity() {
    }

    public RoomEntity(WardEntity ward, String roomNumber, Integer capacity, String comfortLevel) {
        this.id = UUID.randomUUID();
        this.ward = ward;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.comfortLevel = comfortLevel;
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

    public WardEntity getWard() {
        return ward;
    }

    public void setWard(WardEntity ward) {
        this.ward = ward;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getComfortLevel() {
        return comfortLevel;
    }

    public void setComfortLevel(String comfortLevel) {
        this.comfortLevel = comfortLevel;
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
}
