package com.joprelys.backend.ai.facts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_clinical_fact_revision_evidence")
public class ClinicalFactRevisionEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "transcript_item_id", nullable = false)
    private UUID transcriptItemId;

    @Column(name = "quote_start_char", nullable = false)
    private int quoteStartChar;

    @Column(name = "quote_end_char", nullable = false)
    private int quoteEndChar;

    @Column(name = "quote_text", nullable = false, columnDefinition = "TEXT")
    private String quoteText;

    @Column(name = "primary_support", nullable = false)
    private boolean primarySupport;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClinicalFactRevisionEvidenceEntity() {
    }

    public ClinicalFactRevisionEvidenceEntity(
            UUID operationId,
            UUID transcriptItemId,
            int quoteStartChar,
            int quoteEndChar,
            String quoteText,
            boolean primarySupport) {
        this.id = UUID.randomUUID();
        this.operationId = operationId;
        this.transcriptItemId = transcriptItemId;
        this.quoteStartChar = quoteStartChar;
        this.quoteEndChar = quoteEndChar;
        this.quoteText = quoteText;
        this.primarySupport = primarySupport;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOperationId() { return operationId; }
    public UUID getTranscriptItemId() { return transcriptItemId; }
    public int getQuoteStartChar() { return quoteStartChar; }
    public int getQuoteEndChar() { return quoteEndChar; }
    public String getQuoteText() { return quoteText; }
    public boolean isPrimarySupport() { return primarySupport; }
    public Instant getCreatedAt() { return createdAt; }
}
