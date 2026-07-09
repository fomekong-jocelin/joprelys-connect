package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "medication_administrations")
public class MedicationAdministrationEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @Column(name = "prescription_item_id")
    private UUID prescriptionItemId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "dose", nullable = false, length = 100)
    private String dose;

    @Column(name = "administered_by", nullable = false, length = 100)
    private String administeredBy;

    @Column(name = "administered_at", nullable = false)
    private Instant administeredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MedicationAdministrationEntity() {
    }

    public MedicationAdministrationEntity(UUID hospitalizationId, UUID prescriptionItemId, String medicationName, String dose, String administeredBy, Instant administeredAt) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.prescriptionItemId = prescriptionItemId;
        this.medicationName = medicationName;
        this.dose = dose;
        this.administeredBy = administeredBy;
        this.administeredAt = administeredAt;
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

    public UUID getId() { return id; }
    public UUID getHospitalizationId() { return hospitalizationId; }
    public UUID getPrescriptionItemId() { return prescriptionItemId; }
    public UUID getOrganizationId() { return organizationId; }
    public Long getVersion() { return version; }
    public String getMedicationName() { return medicationName; }
    public String getDose() { return dose; }
    public String getAdministeredBy() { return administeredBy; }
    public Instant getAdministeredAt() { return administeredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
