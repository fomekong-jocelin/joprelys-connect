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
@Table(name = "patient_allergies")
public class PatientAllergyEntity {

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

    @Column(name = "substance", nullable = false)
    private String substance;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "reaction")
    private String reaction;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // ACTIVE, INACTIVE

    @Column(name = "discovered_at")
    private LocalDate discoveredAt;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientAllergyEntity() {
    }

    public PatientAllergyEntity(UUID patientId, String substance, String severity, String reaction, String status, LocalDate discoveredAt, String comment) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.substance = substance;
        this.severity = severity != null ? severity : "MEDIUM";
        this.reaction = reaction;
        this.status = status != null ? status : "ACTIVE";
        this.discoveredAt = discoveredAt;
        this.comment = comment;
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
    public String getSubstance() { return substance; }
    public String getSeverity() { return severity; }
    public String getReaction() { return reaction; }
    public String getStatus() { return status; }
    public LocalDate getDiscoveredAt() { return discoveredAt; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setSubstance(String substance) { this.substance = substance; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setReaction(String reaction) { this.reaction = reaction; }
    public void setStatus(String status) { this.status = status; }
    public void setDiscoveredAt(LocalDate date) { this.discoveredAt = date; }
    public void setComment(String comment) { this.comment = comment; }
    public void setPatientId(java.util.UUID patientId) { this.patientId = patientId; }
}
