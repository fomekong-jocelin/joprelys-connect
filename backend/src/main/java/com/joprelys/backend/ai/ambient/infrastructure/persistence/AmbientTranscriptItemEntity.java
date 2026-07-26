package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSource;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSpeaker;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptStatus;
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
@Table(name = "ai_ambient_transcript_items")
public class AmbientTranscriptItemEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @Column(name = "source_event_id", nullable = false, length = 200)
    private String sourceEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private AmbientTranscriptSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker_type", nullable = false, length = 16)
    private AmbientTranscriptSpeaker speakerType;

    @Column(name = "speaker_label", length = 64)
    private String speakerLabel;

    @Column(name = "transcript_text", nullable = false, columnDefinition = "TEXT")
    private String transcriptText;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Column(name = "start_offset_ms", nullable = false)
    private long startOffsetMs;

    @Column(name = "end_offset_ms", nullable = false)
    private long endOffsetMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AmbientTranscriptStatus status;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "supersedes_item_id")
    private UUID supersedesItemId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AmbientTranscriptItemEntity() {
    }

    public AmbientTranscriptItemEntity(
            UUID organizationId,
            UUID visitId,
            long sequenceNo,
            String sourceEventId,
            AmbientTranscriptSource source,
            AmbientTranscriptSpeaker speakerType,
            String speakerLabel,
            String transcriptText,
            String locale,
            long startOffsetMs,
            long endOffsetMs,
            AmbientTranscriptStatus status,
            UUID createdByUserId,
            UUID supersedesItemId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.sequenceNo = sequenceNo;
        this.sourceEventId = sourceEventId;
        this.source = source;
        this.speakerType = speakerType;
        this.speakerLabel = speakerLabel;
        this.transcriptText = transcriptText;
        this.locale = locale;
        this.startOffsetMs = startOffsetMs;
        this.endOffsetMs = endOffsetMs;
        this.status = status;
        this.createdByUserId = createdByUserId;
        this.supersedesItemId = supersedesItemId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public AmbientTranscriptSource getSource() {
        return source;
    }

    public AmbientTranscriptSpeaker getSpeakerType() {
        return speakerType;
    }

    public String getSpeakerLabel() {
        return speakerLabel;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public String getLocale() {
        return locale;
    }

    public long getStartOffsetMs() {
        return startOffsetMs;
    }

    public long getEndOffsetMs() {
        return endOffsetMs;
    }

    public AmbientTranscriptStatus getStatus() {
        return status;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public UUID getSupersedesItemId() {
        return supersedesItemId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
