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

    @Column(name = "current_service_unit_id", nullable = false)
    private UUID currentServiceUnitId;

    @Column(name = "current_space_id", nullable = false)
    private UUID currentSpaceId;

    @Column(name = "current_bed_id", nullable = false)
    private UUID currentBedId;

    @Column(name = "service_name_snapshot", nullable = false, length = 120)
    private String serviceNameSnapshot;

    @Column(name = "space_name_snapshot", nullable = false, length = 120)
    private String spaceNameSnapshot;

    @Column(name = "bed_number_snapshot", nullable = false, length = 50)
    private String bedNumberSnapshot;

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

    @Column(name = "discharge_decided_at")
    private Instant dischargeDecidedAt;

    @Column(name = "discharge_decided_by")
    private UUID dischargeDecidedBy;

    @Column(name = "discharge_against_medical_advice", nullable = false)
    private Boolean dischargeAgainstMedicalAdvice;

    @Column(name = "physical_departure_at")
    private Instant physicalDepartureAt;

    @Column(name = "physical_departure_by")
    private UUID physicalDepartureBy;

    @Column(name = "physical_departure_note", length = 500)
    private String physicalDepartureNote;

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
            UUID currentServiceUnitId,
            UUID currentSpaceId,
            UUID currentBedId,
            String serviceNameSnapshot,
            String spaceNameSnapshot,
            String bedNumberSnapshot,
            String admissionReason,
            String hospitalizationNumber,
            UUID visitId,
            UUID emergencyId,
            UUID responsiblePractitionerId) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.currentServiceUnitId = currentServiceUnitId;
        this.currentSpaceId = currentSpaceId;
        this.currentBedId = currentBedId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.spaceNameSnapshot = spaceNameSnapshot;
        this.bedNumberSnapshot = bedNumberSnapshot;
        this.admissionReason = admissionReason;
        this.hospitalizationNumber = hospitalizationNumber;
        this.visitId = visitId;
        this.emergencyId = emergencyId;
        this.responsiblePractitionerId = responsiblePractitionerId;
        this.status = "EN_COURS";
        this.admittedAt = Instant.now();
        this.dischargeAgainstMedicalAdvice = false;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (dischargeAgainstMedicalAdvice == null) {
            dischargeAgainstMedicalAdvice = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void relocate(
            UUID serviceUnitId,
            UUID spaceId,
            UUID bedId,
            String serviceNameSnapshot,
            String spaceNameSnapshot,
            String bedNumberSnapshot) {
        this.currentServiceUnitId = serviceUnitId;
        this.currentSpaceId = spaceId;
        this.currentBedId = bedId;
        this.serviceNameSnapshot = serviceNameSnapshot;
        this.spaceNameSnapshot = spaceNameSnapshot;
        this.bedNumberSnapshot = bedNumberSnapshot;
    }

    public void decideDischarge(
            String diagnosis,
            String instructions,
            boolean againstMedicalAdvice,
            UUID decidedBy,
            Instant decidedAt) {
        this.dischargeDiagnosis = diagnosis;
        this.dischargeInstructions = instructions;
        this.dischargeAgainstMedicalAdvice = againstMedicalAdvice;
        this.dischargeDecidedBy = decidedBy;
        this.dischargeDecidedAt = decidedAt;
    }

    public void confirmPhysicalDeparture(UUID confirmedBy, String note, Instant confirmedAt) {
        this.physicalDepartureBy = confirmedBy;
        this.physicalDepartureNote = note;
        this.physicalDepartureAt = confirmedAt;
    }

    public boolean hasDischargeDecision() {
        return dischargeDecidedAt != null;
    }

    public boolean hasPhysicalDeparture() {
        return physicalDepartureAt != null;
    }

    public void discharge(String diagnosis, String instructions, String pdfPath) {
        discharge(diagnosis, instructions, pdfPath, "SORTI");
    }

    public void discharge(String diagnosis, String instructions, String pdfPath, String finalStatus) {
        if (physicalDepartureAt == null) {
            throw new IllegalStateException(
                    "Le séjour ne peut pas être clôturé avant la confirmation du départ physique.");
        }
        this.status = finalStatus;
        this.dischargedAt = physicalDepartureAt;
        this.dischargeDiagnosis = diagnosis;
        this.dischargeInstructions = instructions;
        this.pdfFilePath = pdfPath;
    }

    public UUID getId() { return id; }
    public UUID getPatientId() { return patientId; }
    public UUID getOrganizationId() { return organizationId; }
    public Long getVersion() { return version; }
    public UUID getCurrentServiceUnitId() { return currentServiceUnitId; }
    public UUID getCurrentSpaceId() { return currentSpaceId; }
    public UUID getCurrentBedId() { return currentBedId; }
    public String getServiceName() { return serviceNameSnapshot; }
    public String getSpaceName() { return spaceNameSnapshot; }
    public String getBedNumber() { return bedNumberSnapshot; }
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
    public Instant getDischargeDecidedAt() { return dischargeDecidedAt; }
    public UUID getDischargeDecidedBy() { return dischargeDecidedBy; }
    public Boolean getDischargeAgainstMedicalAdvice() { return dischargeAgainstMedicalAdvice; }
    public Instant getPhysicalDepartureAt() { return physicalDepartureAt; }
    public UUID getPhysicalDepartureBy() { return physicalDepartureBy; }
    public String getPhysicalDepartureNote() { return physicalDepartureNote; }
    public String getPdfFilePath() { return pdfFilePath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public void setAdmissionReason(String reason) { this.admissionReason = reason; }
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
    public void setHospitalizationNumber(String hospitalizationNumber) { this.hospitalizationNumber = hospitalizationNumber; }
    public void setVisitId(UUID visitId) { this.visitId = visitId; }
    public void setEmergencyId(UUID emergencyId) { this.emergencyId = emergencyId; }
    public void setResponsiblePractitionerId(UUID responsiblePractitionerId) { this.responsiblePractitionerId = responsiblePractitionerId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
}
