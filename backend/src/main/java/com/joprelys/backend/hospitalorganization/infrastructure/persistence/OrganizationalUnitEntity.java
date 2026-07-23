package com.joprelys.backend.hospitalorganization.infrastructure.persistence;

import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
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
@Table(name = "organizational_units")
public class OrganizationalUnitEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(length = 120)
    private String name;

    @Convert(converter = OrganizationalUnitTypeConverter.class)
    @Column(name = "unit_type", nullable = false, length = 32)
    private OrganizationalUnitType unitType;

    @Column(name = "service_catalog_code", length = 64)
    private String serviceCatalogCode;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationalUnitEntity() {
    }

    public OrganizationalUnitEntity(
            UUID organizationId,
            UUID parentId,
            String code,
            String name,
            OrganizationalUnitType unitType,
            String serviceCatalogCode) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.parentId = parentId;
        this.code = code;
        this.unitType = unitType;
        this.name = normalizedName(name, unitType);
        this.serviceCatalogCode = serviceCatalogCode;
        this.active = true;
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

    public void update(UUID parentId, String code, String name, String serviceCatalogCode) {
        this.parentId = parentId;
        this.code = code;
        this.name = normalizedName(name, unitType);
        this.serviceCatalogCode = serviceCatalogCode;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public OrganizationalUnitType getUnitType() {
        return unitType;
    }

    public String getServiceCatalogCode() {
        return serviceCatalogCode;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizedName(String value, OrganizationalUnitType type) {
        return type == OrganizationalUnitType.SERVICE ? null : value;
    }
}
