package com.joprelys.backend.emergency.triage.infrastructure.persistence;

import com.joprelys.backend.emergency.infrastructure.persistence.EmergencyEntity;
import com.joprelys.backend.emergency.triage.application.EmergencyTriageAssessmentCommand;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AirwayStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.AssessmentType;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.BreathingStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.CirculationStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.DisabilityStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.ExposureStatus;
import com.joprelys.backend.emergency.triage.domain.EmergencyTriageVocabulary.RecommendedOrientation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_triage_assessments")
public class EmergencyTriageAssessmentEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emergency_id", nullable = false)
    private EmergencyEntity emergency;

    @Column(name = "assessment_type", nullable = false, length = 20)
    private String assessmentType;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "triage_level", nullable = false, length = 20)
    private String triageLevel;

    @Column(name = "hemodynamic_status", nullable = false, length = 50)
    private String hemodynamicStatus;

    @Column(name = "airway_status", nullable = false, length = 32)
    private String airwayStatus;

    @Column(name = "breathing_status", nullable = false, length = 32)
    private String breathingStatus;

    @Column(name = "circulation_status", nullable = false, length = 32)
    private String circulationStatus;

    @Column(name = "disability_status", nullable = false, length = 32)
    private String disabilityStatus;

    @Column(name = "exposure_status", nullable = false, length = 32)
    private String exposureStatus;

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "oxygen_saturation")
    private Integer oxygenSaturation;

    @Column(name = "temperature", precision = 4, scale = 2)
    private BigDecimal temperature;

    @Column(name = "gcs_score")
    private Integer gcsScore;

    @Column(name = "pain_score")
    private Integer painScore;

    @Column(name = "recommended_orientation", length = 32)
    private String recommendedOrientation;

    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Column(name = "assessed_by_user_id")
    private UUID assessedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmergencyTriageAssessmentEntity() {
    }

    private EmergencyTriageAssessmentEntity(
            EmergencyEntity emergency,
            AssessmentType assessmentType,
            int sequenceNumber,
            EmergencyTriageAssessmentCommand command,
            UUID actorId) {
        this.id = UUID.randomUUID();
        this.organizationId = emergency.getOrganizationId();
        this.emergency = emergency;
        this.assessmentType = requireEnumName(assessmentType);
        this.sequenceNumber = sequenceNumber;
        this.triageLevel = requireText(command.triageLevel());
        this.hemodynamicStatus = requireText(command.hemodynamicStatus());
        this.airwayStatus = requireEnumName(command.airwayStatus());
        this.breathingStatus = requireEnumName(command.breathingStatus());
        this.circulationStatus = requireEnumName(command.circulationStatus());
        this.disabilityStatus = requireEnumName(command.disabilityStatus());
        this.exposureStatus = requireEnumName(command.exposureStatus());
        this.bpSystolic = command.bpSystolic();
        this.bpDiastolic = command.bpDiastolic();
        this.heartRate = command.heartRate();
        this.respiratoryRate = command.respiratoryRate();
        this.oxygenSaturation = command.oxygenSaturation();
        this.temperature = command.temperature();
        this.gcsScore = command.gcsScore();
        this.painScore = command.painScore();
        this.recommendedOrientation = enumName(command.recommendedOrientation());
        this.clinicalNotes = normalize(command.clinicalNotes());
        this.assessedAt = command.assessedAt() == null ? Instant.now() : command.assessedAt();
        this.assessedByUserId = actorId;
    }

    public static EmergencyTriageAssessmentEntity initial(
            EmergencyEntity emergency,
            EmergencyTriageAssessmentCommand command,
            UUID actorId) {
        return new EmergencyTriageAssessmentEntity(
                emergency,
                AssessmentType.INITIAL,
                1,
                command,
                actorId);
    }

    public static EmergencyTriageAssessmentEntity reassessment(
            EmergencyEntity emergency,
            int sequenceNumber,
            EmergencyTriageAssessmentCommand command,
            UUID actorId) {
        return new EmergencyTriageAssessmentEntity(
                emergency,
                AssessmentType.REASSESSMENT,
                sequenceNumber,
                command,
                actorId);
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private static String requireText(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException("EMERGENCY_TRIAGE_VALUE_REQUIRED");
        }
        return normalized;
    }

    private static String requireEnumName(Enum<?> value) {
        String name = enumName(value);
        if (name == null) {
            throw new IllegalArgumentException("EMERGENCY_TRIAGE_VALUE_REQUIRED");
        }
        return name;
    }

    private static String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public EmergencyEntity getEmergency() { return emergency; }
    public AssessmentType getAssessmentType() { return AssessmentType.valueOf(assessmentType); }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getTriageLevel() { return triageLevel; }
    public String getHemodynamicStatus() { return hemodynamicStatus; }
    public AirwayStatus getAirwayStatus() { return AirwayStatus.valueOf(airwayStatus); }
    public BreathingStatus getBreathingStatus() { return BreathingStatus.valueOf(breathingStatus); }
    public CirculationStatus getCirculationStatus() { return CirculationStatus.valueOf(circulationStatus); }
    public DisabilityStatus getDisabilityStatus() { return DisabilityStatus.valueOf(disabilityStatus); }
    public ExposureStatus getExposureStatus() { return ExposureStatus.valueOf(exposureStatus); }
    public Integer getBpSystolic() { return bpSystolic; }
    public Integer getBpDiastolic() { return bpDiastolic; }
    public Integer getHeartRate() { return heartRate; }
    public Integer getRespiratoryRate() { return respiratoryRate; }
    public Integer getOxygenSaturation() { return oxygenSaturation; }
    public BigDecimal getTemperature() { return temperature; }
    public Integer getGcsScore() { return gcsScore; }
    public Integer getPainScore() { return painScore; }
    public RecommendedOrientation getRecommendedOrientation() {
        return recommendedOrientation == null ? null : RecommendedOrientation.valueOf(recommendedOrientation);
    }
    public String getClinicalNotes() { return clinicalNotes; }
    public Instant getAssessedAt() { return assessedAt; }
    public UUID getAssessedByUserId() { return assessedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}