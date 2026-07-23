package com.joprelys.backend.spatial.infrastructure.persistence;

import com.joprelys.backend.spatial.domain.FacilityLocationNodeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "facility_location_nodes")
public class FacilityLocationNodeEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Convert(converter = FacilityLocationNodeTypeConverter.class)
    @Column(name = "node_type", nullable = false, length = 32)
    private FacilityLocationNodeType nodeType;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FacilityLocationNodeEntity() {
    }

    public FacilityLocationNodeEntity(
            UUID organizationId,
            UUID parentId,
            String code,
            String name,
            FacilityLocationNodeType nodeType) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.parentId = parentId;
        this.code = code;
        this.name = name;
        this.nodeType = nodeType;
        this.active = true;
    }

    public void update(UUID parentId, String code, String name) {
        this.parentId = parentId;
        this.code = code;
        this.name = name;
    }

    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

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
    public UUID getParentId() { return parentId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public FacilityLocationNodeType getNodeType() { return nodeType; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
