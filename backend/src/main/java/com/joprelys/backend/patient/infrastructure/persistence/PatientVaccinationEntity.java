package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient_vaccinations")
public class PatientVaccinationEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "vaccine_name", nullable = false)
    private String vaccineName;

    @Column(name = "batch_number", length = 50)
    private String batchNumber;

    @Column(name = "administered_at", nullable = false)
    private LocalDate administeredAt;

    @Column(name = "administered_by")
    private String administeredBy;

    @Column(name = "notes")
    private String notes;

    @Column(name = "next_dose_at")
    private LocalDate nextDoseAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientVaccinationEntity() {
    }

    public PatientVaccinationEntity(UUID patientId, String vaccineName, String batchNumber, LocalDate administeredAt, String administeredBy, String notes, LocalDate nextDoseAt) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.vaccineName = vaccineName;
        this.batchNumber = batchNumber;
        this.administeredAt = administeredAt;
        this.administeredBy = administeredBy;
        this.notes = notes;
        this.nextDoseAt = nextDoseAt;
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
    public UUID getOrganizationId() { return organizationId; }
    public Long getVersion() { return version; }
    public UUID getPatientId() { return patientId; }
    public String getVaccineName() { return vaccineName; }
    public String getBatchNumber() { return batchNumber; }
    public LocalDate getAdministeredAt() { return administeredAt; }
    public String getAdministeredBy() { return administeredBy; }
    public String getNotes() { return notes; }
    public LocalDate getNextDoseAt() { return nextDoseAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public void setAdministeredAt(LocalDate administeredAt) { this.administeredAt = administeredAt; }
    public void setAdministeredBy(String administeredBy) { this.administeredBy = administeredBy; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setNextDoseAt(LocalDate nextDoseAt) { this.nextDoseAt = nextDoseAt; }
}
