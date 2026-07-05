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
@Table(name = "patient_medical_history")
public class PatientMedicalHistoryEntity {

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

    @Column(name = "category", nullable = false, length = 50)
    private String category; // MEDICAL, SURGICAL, FAMILY, OBSTETRICAL, OTHER

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "is_ongoing", nullable = false)
    private boolean isOngoing;

    @Column(name = "comment")
    private String comment;

    @Column(name = "important", nullable = false)
    private boolean important;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PatientMedicalHistoryEntity() {
    }

    public PatientMedicalHistoryEntity(UUID patientId, String category, String description, LocalDate onsetDate, boolean isOngoing, String comment) {
        this(patientId, category, description, onsetDate, isOngoing, comment, false);
    }

    public PatientMedicalHistoryEntity(UUID patientId, String category, String description, LocalDate onsetDate, boolean isOngoing, String comment, boolean important) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.category = category != null ? category : "OTHER";
        this.description = description;
        this.onsetDate = onsetDate;
        this.isOngoing = isOngoing;
        this.comment = comment;
        this.important = important;
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
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public LocalDate getOnsetDate() { return onsetDate; }
    public boolean isOngoing() { return isOngoing; }
    public String getComment() { return comment; }
    public boolean isImportant() { return important; }
    public Instant getDeletedAt() { return deletedAt; }
    public UUID getDeletedBy() { return deletedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCategory(String category) { this.category = category; }
    public void setDescription(String description) { this.description = description; }
    public void setOnsetDate(LocalDate date) { this.onsetDate = date; }
    public void setOngoing(boolean ongoing) { this.isOngoing = ongoing; }
    public void setComment(String comment) { this.comment = comment; }
    public void setPatientId(java.util.UUID patientId) { this.patientId = patientId; }
    public void setImportant(boolean important) { this.important = important; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }
}
