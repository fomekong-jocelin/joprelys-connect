package com.joprelys.backend.prescription.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stock physique d'un médicament pour une clinique (tenant).
 * Utilise @Version pour le verrouillage optimiste JPA (STORY-1103).
 */
@Entity
@Table(name = "drug_stocks")
public class DrugStockEntity {

    @Id
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "drug_name", nullable = false, length = 255)
    private String drugName;

    @Column(name = "generic_name", length = 255)
    private String genericName;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "minimum_threshold", nullable = false)
    private Integer minimumThreshold;

    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "supplier", length = 255)
    private String supplier;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DrugStockEntity() {}

    public DrugStockEntity(String drugName, String genericName,
                           String unit, int quantityAvailable, int minimumThreshold,
                           String batchNumber, LocalDate expiryDate, String supplier) {
        this.id = UUID.randomUUID();
        this.drugName = drugName;
        this.genericName = genericName;
        this.unit = unit != null ? unit : "comprime";
        this.quantityAvailable = quantityAvailable;
        this.minimumThreshold = minimumThreshold;
        this.batchNumber = batchNumber;
        this.expiryDate = expiryDate;
        this.supplier = supplier;
        // organizationId est injecté automatiquement par Hibernate via @TenantId
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /** Décrémentation sécurisée avec vérification de la disponibilité. */
    public void decrementStock(int quantity) {
        if (quantity <= 0) return;
        if (this.quantityAvailable < quantity) {
            throw new IllegalStateException(
                "Stock insuffisant pour " + drugName +
                " (disponible: " + quantityAvailable + ", demandé: " + quantity + ")");
        }
        this.quantityAvailable -= quantity;
    }

    public boolean isBelowThreshold() {
        return this.quantityAvailable <= this.minimumThreshold;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public UUID getOrganizationId() { return organizationId; }
    public String getDrugName() { return drugName; }
    public String getGenericName() { return genericName; }
    public String getUnit() { return unit; }
    public Integer getQuantityAvailable() { return quantityAvailable; }
    public Integer getMinimumThreshold() { return minimumThreshold; }
    public String getBatchNumber() { return batchNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getSupplier() { return supplier; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setQuantityAvailable(Integer q) { this.quantityAvailable = q; }
    public void setDrugName(String n) { this.drugName = n; }
    public void setGenericName(String n) { this.genericName = n; }
    public void setUnit(String u) { this.unit = u; }
    public void setMinimumThreshold(Integer t) { this.minimumThreshold = t; }
    public void setBatchNumber(String b) { this.batchNumber = b; }
    public void setExpiryDate(LocalDate d) { this.expiryDate = d; }
    public void setSupplier(String s) { this.supplier = s; }
}
