package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

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

    @Column(name = "hospitalization_number", nullable = false, length = 50)
    private String hospitalizationNumber;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "emergency_id")
    private UUID emergencyId;

    @Column(name = "responsible_practitioner_id")
    private UUID responsiblePractitionerId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

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

    public HospitalizationEntity(
            UUID patientId,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String admissionReason) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.serviceName = serviceName;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.admissionReason = admissionReason;
        this.status = "EN_COURS";
        this.admittedAt = Instant.now();
        this.hospitalizationNumber = "HOSP-TEMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public HospitalizationEntity(
            UUID patientId,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String admissionReason,
            String hospitalizationNumber,
            UUID visitId,
            UUID responsiblePractitionerId) {
        this(
                patientId,
                serviceName,
                roomNumber,
                bedNumber,
                admissionReason,
                hospitalizationNumber,
                visitId,
                null,
                responsiblePractitionerId);
    }

    public HospitalizationEntity(
            UUID patientId,
            String serviceName,
            String roomNumber,
            String bedNumber,
            String admissionReason,
            String hospitalizationNumber,
            UUID visitId,
            UUID emergencyId,
            UUID responsiblePractitionerId) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.serviceName = serviceName;
        this.roomNumber = roomNumber;
        this.bedNumber = bedNumber;
        this.admissionReason = admissionReason;
        this.hospitalizationNumber = hospitalizationNumber;
        this.visitId = visitId;
        this.emergencyId = emergencyId;
        this.responsiblePractitionerId = responsiblePractitionerId;
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
        discharge(diagnosis, instructions, pdfPath, "SORTI");
    }

    public void discharge(String diagnosis, String instructions, String pdfPath, String finalStatus) {
        this.status = finalStatus;
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
    public String getHospitalizationNumber() { return hospitalizationNumber; }
    public UUID getVisitId() { return visitId; }
    public UUID getEmergencyId() { return emergencyId; }
    public UUID getResponsiblePractitionerId() { return responsiblePractitionerId; }
    public UUID getDocumentId() { return documentId; }
    public String getStatus() { return status; }
    public Instant getAdmittedAt() { return admittedAt; }
    public Instant getDischargedAt() { return dischargedAt; }
    public String getDischargeDiagnosis() { return dischargeDiagnosis; }
    public String getDischargeInstructions() { return dischargeInstructions; }
    public String getPdfFilePath() { return pdfFilePath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public void setAdmissionReason(String reason) { this.admissionReason = reason; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public void setHospitalizationNumber(String hospitalizationNumber) { this.hospitalizationNumber = hospitalizationNumber; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public void setEmergencyId(UUID emergencyId) { this.emergencyId = emergencyId; }
    public void setResponsiblePractitionerId(UUID responsiblePractitionerId) { this.responsiblePractitionerId = responsiblePractitionerId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
}
