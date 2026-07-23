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
@Table(name = "facility_spaces")
public class FacilitySpaceEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "location_node_id")
    private UUID locationNodeId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "space_type_code", nullable = false, length = 64)
    private String spaceTypeCode;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FacilitySpaceEntity() {
    }

    public FacilitySpaceEntity(
            UUID organizationId,
            UUID locationNodeId,
            String code,
            String name,
            String spaceTypeCode) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.locationNodeId = locationNodeId;
        this.code = code;
        this.name = name;
        this.spaceTypeCode = spaceTypeCode;
        this.active = true;
    }

    public void update(UUID locationNodeId, String code, String name, String spaceTypeCode) {
        this.locationNodeId = locationNodeId;
        this.code = code;
        this.name = name;
        this.spaceTypeCode = spaceTypeCode;
    }

    public void activate() { active = true; }
    public void deactivate() { active = false; }

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
    public UUID getLocationNodeId() { return locationNodeId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSpaceTypeCode() { return spaceTypeCode; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
