package com.joprelys.backend.billing.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tariff_grid", uniqueConstraints = {
        @UniqueConstraint(name = "uq_key_letter_org", columnNames = {"key_letter", "organization_id"})
})
public class TariffGridEntity {

    @Id
    private UUID id;

    @Column(name = "key_letter", nullable = false, length = 100)
    private String keyLetter;

    @Column(name = "unit_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal unitValue;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TariffGridEntity() {
    }

    public TariffGridEntity(String keyLetter, BigDecimal unitValue) {
        this.id = UUID.randomUUID();
        this.keyLetter = keyLetter;
        this.unitValue = unitValue;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { this.updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getKeyLetter() { return keyLetter; }
    public void setKeyLetter(String keyLetter) { this.keyLetter = keyLetter; }
    public BigDecimal getUnitValue() { return unitValue; }
    public void setUnitValue(BigDecimal unitValue) { this.unitValue = unitValue; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
