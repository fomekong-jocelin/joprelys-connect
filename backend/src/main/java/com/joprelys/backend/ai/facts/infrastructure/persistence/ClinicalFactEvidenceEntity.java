package com.joprelys.backend.ai.facts.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_clinical_fact_evidence")
public class ClinicalFactEvidenceEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fact_id", nullable = false)
    private ClinicalFactEntity fact;

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

    protected ClinicalFactEvidenceEntity() {
    }

    public ClinicalFactEvidenceEntity(
            UUID transcriptItemId,
            int quoteStartChar,
            int quoteEndChar,
            String quoteText,
            boolean primarySupport) {
        this.id = UUID.randomUUID();
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

    void attachTo(ClinicalFactEntity fact) {
        this.fact = fact;
    }

    public UUID getId() { return id; }
    public UUID getTranscriptItemId() { return transcriptItemId; }
    public int getQuoteStartChar() { return quoteStartChar; }
    public int getQuoteEndChar() { return quoteEndChar; }
    public String getQuoteText() { return quoteText; }
    public boolean isPrimarySupport() { return primarySupport; }
    public Instant getCreatedAt() { return createdAt; }
}
