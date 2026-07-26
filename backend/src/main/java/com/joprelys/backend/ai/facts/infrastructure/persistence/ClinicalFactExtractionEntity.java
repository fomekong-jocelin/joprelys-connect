package com.joprelys.backend.ai.facts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_clinical_fact_extractions")
public class ClinicalFactExtractionEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "transcript_item_id", nullable = false)
    private UUID transcriptItemId;

    @Column(name = "transcript_sha256", nullable = false, length = 64)
    private String transcriptSha256;

    @Column(name = "extractor_version", nullable = false, length = 32)
    private String extractorVersion;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "candidate_count", nullable = false)
    private int candidateCount;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @Column(name = "rejected_count", nullable = false)
    private int rejectedCount;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected ClinicalFactExtractionEntity() {
    }

    public ClinicalFactExtractionEntity(
            UUID organizationId,
            UUID visitId,
            UUID transcriptItemId,
            String transcriptSha256,
            String extractorVersion,
            String model,
            int candidateCount,
            int acceptedCount,
            int rejectedCount,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.transcriptItemId = transcriptItemId;
        this.transcriptSha256 = transcriptSha256;
        this.extractorVersion = extractorVersion;
        this.model = model;
        this.candidateCount = candidateCount;
        this.acceptedCount = acceptedCount;
        this.rejectedCount = rejectedCount;
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        if (completedAt == null) completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public UUID getTranscriptItemId() { return transcriptItemId; }
    public String getTranscriptSha256() { return transcriptSha256; }
    public String getExtractorVersion() { return extractorVersion; }
    public String getModel() { return model; }
    public int getCandidateCount() { return candidateCount; }
    public int getAcceptedCount() { return acceptedCount; }
    public int getRejectedCount() { return rejectedCount; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCompletedAt() { return completedAt; }
}
