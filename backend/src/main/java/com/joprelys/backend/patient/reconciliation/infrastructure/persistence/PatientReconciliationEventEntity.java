package com.joprelys.backend.patient.reconciliation.infrastructure.persistence;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import com.joprelys.backend.patient.domain.PatientIdentityStatus;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.reconciliation.domain.PatientReconciliationDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.TenantId;

@Entity
@Immutable
@Table(name = "patient_reconciliation_events")
public class PatientReconciliationEventEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_patient_id", nullable = false)
    private PatientEntity sourcePatient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_patient_id")
    private PatientEntity candidatePatient;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private PatientReconciliationDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_identity_status", nullable = false, length = 32)
    private PatientIdentityStatus previousIdentityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_identity_status", nullable = false, length = 32)
    private PatientIdentityStatus resultingIdentityStatus;

    @Column(name = "similarity_score", precision = 5, scale = 2)
    private BigDecimal similarityScore;

    @Column(name = "match_reasons", columnDefinition = "TEXT")
    private String matchReasons;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_source_type", nullable = false, length = 48)
    private IdentitySourceType evidenceSourceType;

    @Column(name = "evidence_reference", length = 255)
    private String evidenceReference;

    @Column(name = "justification", nullable = false, length = 1500)
    private String justification;

    @Column(name = "corrected_event_id")
    private UUID correctedEventId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PatientReconciliationEventEntity() {
    }

    public PatientReconciliationEventEntity(
            UUID organizationId,
            PatientEntity sourcePatient,
            PatientEntity candidatePatient,
            PatientReconciliationDecision decision,
            PatientIdentityStatus previousIdentityStatus,
            PatientIdentityStatus resultingIdentityStatus,
            BigDecimal similarityScore,
            String matchReasons,
            IdentitySourceType evidenceSourceType,
            String evidenceReference,
            String justification,
            UUID correctedEventId,
            String idempotencyKey,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = requireValue(organizationId, "TENANT_CONTEXT_REQUIRED");
        this.sourcePatient = requireValue(sourcePatient, "PATIENT_RECONCILIATION_SOURCE_REQUIRED");
        this.decision = requireValue(decision, "PATIENT_RECONCILIATION_DECISION_REQUIRED");
        validateDecisionShape(this.decision, candidatePatient, correctedEventId);
        validateDistinctPatients(sourcePatient, candidatePatient);
        validateSimilarityScore(similarityScore);
        this.candidatePatient = candidatePatient;
        this.previousIdentityStatus = requireValue(
                previousIdentityStatus,
                "PATIENT_RECONCILIATION_PREVIOUS_STATUS_REQUIRED");
        this.resultingIdentityStatus = requireValue(
                resultingIdentityStatus,
                "PATIENT_RECONCILIATION_RESULTING_STATUS_REQUIRED");
        this.similarityScore = similarityScore;
        this.matchReasons = normalize(matchReasons);
        this.evidenceSourceType = requireValue(
                evidenceSourceType,
                "PATIENT_RECONCILIATION_EVIDENCE_SOURCE_REQUIRED");
        this.evidenceReference = normalize(evidenceReference);
        this.justification = requireText(justification, "PATIENT_RECONCILIATION_JUSTIFICATION_REQUIRED");
        this.correctedEventId = correctedEventId;
        this.idempotencyKey = requireText(idempotencyKey, "IDEMPOTENCY_KEY_REQUIRED");
        this.createdByUserId = requireValue(createdByUserId, "AUTHENTICATION_REQUIRED");
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    private static void validateDecisionShape(
            PatientReconciliationDecision decision,
            PatientEntity candidatePatient,
            UUID correctedEventId) {
        boolean valid = switch (decision) {
            case LINK_EXISTING_DPU -> candidatePatient != null && correctedEventId == null;
            case CREATE_NEW_DPU, DEFER -> candidatePatient == null && correctedEventId == null;
            case CORRECT_LINK -> correctedEventId != null;
        };
        if (!valid) {
            throw new IllegalArgumentException("PATIENT_RECONCILIATION_DECISION_SHAPE_INVALID");
        }
    }

    private static void validateDistinctPatients(
            PatientEntity sourcePatient,
            PatientEntity candidatePatient) {
        if (candidatePatient != null && sourcePatient.getId().equals(candidatePatient.getId())) {
            throw new IllegalArgumentException("PATIENT_RECONCILIATION_SELF_LINK_FORBIDDEN");
        }
    }

    private static void validateSimilarityScore(BigDecimal similarityScore) {
        if (similarityScore != null
                && (similarityScore.signum() < 0 || similarityScore.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("PATIENT_RECONCILIATION_SCORE_INVALID");
        }
    }

    private static String requireText(String value, String errorCode) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(errorCode);
        }
        return normalized;
    }

    private static <T> T requireValue(T value, String errorCode) {
        if (value == null) {
            throw new IllegalArgumentException(errorCode);
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public PatientEntity getSourcePatient() {
        return sourcePatient;
    }

    public PatientEntity getCandidatePatient() {
        return candidatePatient;
    }

    public PatientReconciliationDecision getDecision() {
        return decision;
    }

    public PatientIdentityStatus getPreviousIdentityStatus() {
        return previousIdentityStatus;
    }

    public PatientIdentityStatus getResultingIdentityStatus() {
        return resultingIdentityStatus;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public String getMatchReasons() {
        return matchReasons;
    }

    public IdentitySourceType getEvidenceSourceType() {
        return evidenceSourceType;
    }

    public String getEvidenceReference() {
        return evidenceReference;
    }

    public String getJustification() {
        return justification;
    }

    public UUID getCorrectedEventId() {
        return correctedEventId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}