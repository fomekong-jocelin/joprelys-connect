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
@Table(name = "inpatient_space_profiles")
public class InpatientSpaceProfileEntity {

    @Id
    @Column(name = "space_id")
    private UUID spaceId;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "space_type_code", nullable = false, length = 64)
    private String spaceTypeCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InpatientSpaceProfileEntity() {
    }

    public InpatientSpaceProfileEntity(UUID spaceId, UUID organizationId, String spaceTypeCode) {
        this.spaceId = spaceId;
        this.organizationId = organizationId;
        this.spaceTypeCode = spaceTypeCode;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getSpaceId() { return spaceId; }
    public UUID getOrganizationId() { return organizationId; }
    public String getSpaceTypeCode() { return spaceTypeCode; }
}
