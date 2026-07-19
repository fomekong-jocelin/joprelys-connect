package com.joprelys.backend.spatial.infrastructure.persistence;

import com.joprelys.backend.spatial.domain.HospitalServiceType;
import com.joprelys.backend.spatial.domain.SpatialConfigurationRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "wards")
public class WardEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 40)
    private HospitalServiceType serviceType;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WardEntity() {
    }

    public WardEntity(String name, HospitalServiceType serviceType) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.serviceType = Objects.requireNonNull(serviceType, "Le type de service est obligatoire.");
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

    public void changeServiceType(HospitalServiceType newType, boolean hasRooms) {
        HospitalServiceType requiredType = Objects.requireNonNull(newType, "Le type de service est obligatoire.");
        if (hasRooms && !requiredType.allowsRooms()) {
            throw new SpatialConfigurationRuleException(
                    "Un service contenant des chambres ne peut pas devenir un service non spatial.");
        }
        this.serviceType = requiredType;
    }

    public void requireRoomsAllowed() {
        if (!allowsRooms()) {
            throw new SpatialConfigurationRuleException(
                    "Le type de ce service n'autorise pas la création de chambres.");
        }
    }

    public boolean allowsRooms() {
        return serviceType.allowsRooms();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HospitalServiceType getServiceType() {
        return serviceType;
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
