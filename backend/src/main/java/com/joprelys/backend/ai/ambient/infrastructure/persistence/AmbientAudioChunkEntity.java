package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import com.joprelys.backend.ai.ambient.domain.AmbientAudioChunkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_ambient_audio_chunks")
public class AmbientAudioChunkEntity {

    public static final String EMPTY_DIARIZATION_CONTEXT_SHA256 =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "chunk_id", nullable = false, length = 160)
    private String chunkId;

    @Column(name = "audio_sha256", nullable = false, length = 64)
    private String audioSha256;

    @Column(name = "diarization_context_sha256", nullable = false, length = 64)
    private String diarizationContextSha256;

    @Column(name = "start_offset_ms", nullable = false)
    private long startOffsetMs;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AmbientAudioChunkStatus status;

    @Column(name = "claimed_at", nullable = false)
    private Instant claimedAt;

    @Column(name = "claim_generation", nullable = false)
    private long claimGeneration;

    @Column(name = "claim_token", nullable = false)
    private UUID claimToken;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_error", length = 128)
    private String lastError;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AmbientAudioChunkEntity() {
    }

    public AmbientAudioChunkEntity(
            UUID organizationId,
            UUID visitId,
            String chunkId,
            String audioSha256,
            long startOffsetMs,
            String contentType,
            UUID createdByUserId,
            Instant now) {
        this(
                organizationId,
                visitId,
                chunkId,
                audioSha256,
                EMPTY_DIARIZATION_CONTEXT_SHA256,
                startOffsetMs,
                contentType,
                createdByUserId,
                now);
    }

    public AmbientAudioChunkEntity(
            UUID organizationId,
            UUID visitId,
            String chunkId,
            String audioSha256,
            String diarizationContextSha256,
            long startOffsetMs,
            String contentType,
            UUID createdByUserId,
            Instant now) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.chunkId = chunkId;
        this.audioSha256 = audioSha256;
        this.diarizationContextSha256 = normalizeContextHash(diarizationContextSha256);
        this.startOffsetMs = startOffsetMs;
        this.contentType = contentType;
        this.createdByUserId = createdByUserId;
        this.status = AmbientAudioChunkStatus.PROCESSING;
        this.claimedAt = now;
        this.claimGeneration = 1;
        this.claimToken = UUID.randomUUID();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (claimedAt == null) claimedAt = now;
        if (claimGeneration < 1) claimGeneration = 1;
        if (claimToken == null) claimToken = UUID.randomUUID();
        diarizationContextSha256 = normalizeContextHash(diarizationContextSha256);
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void reclaim(Instant now) {
        status = AmbientAudioChunkStatus.PROCESSING;
        claimedAt = now;
        claimGeneration += 1;
        claimToken = UUID.randomUUID();
        completedAt = null;
        lastError = null;
    }

    public boolean ownsLease(UUID expectedClaimToken) {
        return status == AmbientAudioChunkStatus.PROCESSING
                && expectedClaimToken != null
                && expectedClaimToken.equals(claimToken);
    }

    public void complete(Instant now) {
        status = AmbientAudioChunkStatus.COMPLETED;
        completedAt = now;
        lastError = null;
    }

    public void fail(String errorCode) {
        status = AmbientAudioChunkStatus.FAILED;
        lastError = errorCode == null ? null : errorCode.substring(0, Math.min(128, errorCode.length()));
    }

    private String normalizeContextHash(String hash) {
        return hash == null || hash.isBlank() ? EMPTY_DIARIZATION_CONTEXT_SHA256 : hash;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public String getChunkId() { return chunkId; }
    public String getAudioSha256() { return audioSha256; }
    public String getDiarizationContextSha256() { return diarizationContextSha256; }
    public long getStartOffsetMs() { return startOffsetMs; }
    public String getContentType() { return contentType; }
    public AmbientAudioChunkStatus getStatus() { return status; }
    public Instant getClaimedAt() { return claimedAt; }
    public long getClaimGeneration() { return claimGeneration; }
    public UUID getClaimToken() { return claimToken; }
    public Instant getCompletedAt() { return completedAt; }
    public String getLastError() { return lastError; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
