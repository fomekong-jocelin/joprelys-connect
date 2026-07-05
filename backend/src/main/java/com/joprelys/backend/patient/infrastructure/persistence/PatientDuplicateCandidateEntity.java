package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_duplicate_candidates")
public class PatientDuplicateCandidateEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_patient_id", nullable = false)
    private PatientEntity sourcePatient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_patient_id", nullable = false)
    private PatientEntity targetPatient;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, RESOLVED, IGNORED

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PatientDuplicateCandidateEntity() {}

    public PatientDuplicateCandidateEntity(PatientEntity sourcePatient, PatientEntity targetPatient, double similarityScore) {
        this.id = UUID.randomUUID();
        this.sourcePatient = sourcePatient;
        this.targetPatient = targetPatient;
        this.similarityScore = similarityScore;
        this.status = "PENDING";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public PatientEntity getSourcePatient() { return sourcePatient; }
    public PatientEntity getTargetPatient() { return targetPatient; }
    public double getSimilarityScore() { return similarityScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}