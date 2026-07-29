package com.joprelys.backend.ai.realtime.infrastructure.persistence;

import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_realtime_clinical_intake")
public class RealtimeClinicalIntakeEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RealtimeIntakeSource source;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @Column(name = "event_id", nullable = false, length = 200)
    private String eventId;

    @Column(name = "item_id", length = 200)
    private String itemId;

    @Column(name = "transcript_text", nullable = false, columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "original_transcript_text", columnDefinition = "TEXT")
    private String originalTranscriptText;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "correction_count", nullable = false)
    private int correctionCount;

    @Column(name = "corrected_by_user_id")
    private UUID correctedByUserId;

    @Column(name = "corrected_at")
    private Instant correctedAt;

    @Column(name = "capture_status", nullable = false, length = 16)
    private String captureStatus = "PENDING";

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "discarded_by_user_id")
    private UUID discardedByUserId;

    @Column(name = "discarded_at")
    private Instant discardedAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected RealtimeClinicalIntakeEntity() {
    }

    public RealtimeClinicalIntakeEntity(
            UUID organizationId,
            UUID visitId,
            RealtimeIntakeSource source,
            long sequenceNo,
            String eventId,
            String itemId,
            String transcriptText,
            double confidence,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.source = source;
        this.sequenceNo = sequenceNo;
        this.eventId = eventId;
        this.itemId = itemId;
        this.transcriptText = transcriptText;
        this.confidence = confidence;
        this.createdByUserId = createdByUserId;
        this.captureStatus = "PENDING";
    }

    @PrePersist
    void prePersist() {
        if (receivedAt == null) receivedAt = Instant.now();
        if (captureStatus == null || captureStatus.isBlank()) captureStatus = "PENDING";
    }

    public void correctTranscript(String correctedText, UUID correctedByUserId, Instant correctedAt) {
        if (originalTranscriptText == null) {
            originalTranscriptText = transcriptText;
        }
        transcriptText = correctedText;
        correctionCount += 1;
        this.correctedByUserId = correctedByUserId;
        this.correctedAt = correctedAt;
        captureStatus = "PENDING";
        analyzedAt = null;
        consumedAt = null;
        discardedByUserId = null;
        discardedAt = null;
    }

    public void markAnalyzed(Instant at) {
        if (!"CONSUMED".equals(captureStatus) && !"DISCARDED".equals(captureStatus)) {
            captureStatus = "ANALYZED";
            analyzedAt = at;
        }
    }

    public void markConsumed(Instant at) {
        if (!"DISCARDED".equals(captureStatus)) {
            captureStatus = "CONSUMED";
            consumedAt = at;
        }
    }

    public void discard(UUID userId, Instant at) {
        captureStatus = "DISCARDED";
        discardedByUserId = userId;
        discardedAt = at;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public RealtimeIntakeSource getSource() { return source; }
    public long getSequenceNo() { return sequenceNo; }
    public String getEventId() { return eventId; }
    public String getItemId() { return itemId; }
    public String getTranscriptText() { return transcriptText; }
    public String getOriginalTranscriptText() { return originalTranscriptText; }
    public double getConfidence() { return confidence; }
    public int getCorrectionCount() { return correctionCount; }
    public UUID getCorrectedByUserId() { return correctedByUserId; }
    public Instant getCorrectedAt() { return correctedAt; }
    public String getCaptureStatus() { return captureStatus; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public UUID getDiscardedByUserId() { return discardedByUserId; }
    public Instant getDiscardedAt() { return discardedAt; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getReceivedAt() { return receivedAt; }
}
