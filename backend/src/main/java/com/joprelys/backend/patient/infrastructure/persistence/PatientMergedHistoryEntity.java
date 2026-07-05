package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_merged_history")
public class PatientMergedHistoryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_patient_id", nullable = false)
    private PatientEntity primaryPatient;

    @Column(name = "merged_patient_id", nullable = false)
    private UUID mergedPatientId;

    @Column(name = "merged_patient_dpu", nullable = false, length = 50)
    private String mergedPatientDpu;

    @Column(name = "merged_by", nullable = false)
    private UUID mergedBy;

    @Column(name = "merged_at", nullable = false)
    private Instant mergedAt;

    protected PatientMergedHistoryEntity() {}

    public PatientMergedHistoryEntity(PatientEntity primaryPatient, UUID mergedPatientId, String mergedPatientDpu, UUID mergedBy) {
        this.id = UUID.randomUUID();
        this.primaryPatient = primaryPatient;
        this.mergedPatientId = mergedPatientId;
        this.mergedPatientDpu = mergedPatientDpu;
        this.mergedBy = mergedBy;
        this.mergedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public PatientEntity getPrimaryPatient() { return primaryPatient; }
    public UUID getMergedPatientId() { return mergedPatientId; }
    public String getMergedPatientDpu() { return mergedPatientDpu; }
    public UUID getMergedBy() { return mergedBy; }
    public Instant getMergedAt() { return mergedAt; }
}