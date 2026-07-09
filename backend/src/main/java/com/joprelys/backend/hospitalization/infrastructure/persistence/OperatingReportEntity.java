package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "operating_reports")
public class OperatingReportEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "surgeon_id")
    private UUID surgeonId;

    @Column(name = "anesthetist_id")
    private UUID anesthetistId;

    @Column(name = "operation_date", nullable = false)
    private Instant operationDate;

    @Column(name = "pre_operative_diagnosis", columnDefinition = "TEXT")
    private String preOperativeDiagnosis;

    @Column(name = "post_operative_diagnosis", columnDefinition = "TEXT")
    private String postOperativeDiagnosis;

    @Column(name = "procedure_name", nullable = false)
    private String procedureName;

    @Column(name = "procedure_description", columnDefinition = "TEXT")
    private String procedureDescription;

    @Column(name = "anesthesia_type")
    private String anesthesiaType;

    @Column(name = "anesthesia_description", columnDefinition = "TEXT")
    private String anesthesiaDescription;

    @Column(name = "k_surgeon_value")
    private double kSurgeonValue = 0.0;

    @Column(name = "k_anesthesist_value")
    private double kAnesthesistValue = 0.0;

    @Column(name = "k_bloc_value")
    private double kBlocValue = 0.0;

    @Column(name = "validated", nullable = false)
    private boolean validated = false;

    @Column(name = "validated_by")
    private String validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "operatingReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurgicalImplantEntity> implants = new ArrayList<>();

    public OperatingReportEntity() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public OperatingReportEntity(UUID hospitalizationId, String procedureName, Instant operationDate) {
        this();
        this.hospitalizationId = hospitalizationId;
        this.procedureName = procedureName;
        this.operationDate = operationDate;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHospitalizationId() { return hospitalizationId; }
    public void setHospitalizationId(UUID hospitalizationId) { this.hospitalizationId = hospitalizationId; }

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public UUID getSurgeonId() { return surgeonId; }
    public void setSurgeonId(UUID surgeonId) { this.surgeonId = surgeonId; }

    public UUID getAnesthetistId() { return anesthetistId; }
    public void setAnesthetistId(UUID anesthetistId) { this.anesthetistId = anesthetistId; }

    public Instant getOperationDate() { return operationDate; }
    public void setOperationDate(Instant operationDate) { this.operationDate = operationDate; }

    public String getPreOperativeDiagnosis() { return preOperativeDiagnosis; }
    public void setPreOperativeDiagnosis(String preOperativeDiagnosis) { this.preOperativeDiagnosis = preOperativeDiagnosis; }

    public String getPostOperativeDiagnosis() { return postOperativeDiagnosis; }
    public void setPostOperativeDiagnosis(String postOperativeDiagnosis) { this.postOperativeDiagnosis = postOperativeDiagnosis; }

    public String getProcedureName() { return procedureName; }
    public void setProcedureName(String procedureName) { this.procedureName = procedureName; }

    public String getProcedureDescription() { return procedureDescription; }
    public void setProcedureDescription(String procedureDescription) { this.procedureDescription = procedureDescription; }

    public String getAnesthesiaType() { return anesthesiaType; }
    public void setAnesthesiaType(String anesthesiaType) { this.anesthesiaType = anesthesiaType; }

    public String getAnesthesiaDescription() { return anesthesiaDescription; }
    public void setAnesthesiaDescription(String anesthesiaDescription) { this.anesthesiaDescription = anesthesiaDescription; }

    public double getkSurgeonValue() { return kSurgeonValue; }
    public void setkSurgeonValue(double kSurgeonValue) { this.kSurgeonValue = kSurgeonValue; }

    public double getkAnesthesistValue() { return kAnesthesistValue; }
    public void setkAnesthesistValue(double kAnesthesistValue) { this.kAnesthesistValue = kAnesthesistValue; }

    public double getkBlocValue() { return kBlocValue; }
    public void setkBlocValue(double kBlocValue) { this.kBlocValue = kBlocValue; }

    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }

    public String getValidatedBy() { return validatedBy; }
    public void setValidatedBy(String validatedBy) { this.validatedBy = validatedBy; }

    public Instant getValidatedAt() { return validatedAt; }
    public void setValidatedAt(Instant validatedAt) { this.validatedAt = validatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<SurgicalImplantEntity> getImplants() { return implants; }
    public void setImplants(List<SurgicalImplantEntity> implants) { this.implants = implants; }
}
