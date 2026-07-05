package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitalizations")
public class HospitalizationEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "room_number", nullable = false, length = 50)
    private String roomNumber;

    @Column(name = "bed_number", nullable = false, length = 50)
    private String bedNumber;

    @Column(name = "admission_reason", nullable = false)
    private String admissionReason;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // EN_COURS, SORTI

    @Column(name = "admitted_at", nullable = false)
    private Instant admittedAt;

    @Column(name = "discharged_at")
    private Instant dischargedAt;

    @Column(name = "discharge_diagnosis")
    private String dischargeDiagnosis;

    @Column(name = "discharge_instructions")
    private String dischargeInstructions;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected HospitalizationEntity() {
    }

    public HospitalizationEntity(UUID patientId, String serviceName, String roomNumber, String bedNumber, String admissionReason) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.serviceName = serviceName;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.admissionReason = admissionReason;
        this.status = "EN_COURS";
        this.admittedAt = Instant.now();
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

    public void discharge(String diagnosis, String instructions, String pdfPath) {
        this.status = "SORTI";
        this.dischargedAt = Instant.now();
        this.dischargeDiagnosis = diagnosis;
        this.dischargeInstructions = instructions;
        this.pdfFilePath = pdfPath;
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getOrganizationId() { return organizationId; }
    public Long getVersion() { return version; }
    public String getServiceName() { return serviceName; }
    public String getRoomNumber() { return roomNumber; }
    public String getBedNumber() { return bedNumber; }
    public String getAdmissionReason() { return admissionReason; }
    public String getStatus() { return status; }
    public Instant getAdmittedAt() { return admittedAt; }
    public Instant getDischargedAt() { return dischargedAt; }
    public String getDischargeDiagnosis() { return dischargeDiagnosis; }
    public String getDischargeInstructions() { return dischargeInstructions; }
    public String getPdfFilePath() { return pdfFilePath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public void setAdmissionReason(String reason) { this.admissionReason = reason; }
    public void setPatientId(java.util.UUID patientId) { this.patientId = patientId; }
}
