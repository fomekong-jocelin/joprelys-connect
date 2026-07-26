package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "ai_ambient_note_statements")
public class AmbientNoteStatementEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_revision_id", nullable = false)
    private AmbientNoteRevisionEntity noteRevision;

    @Column(name = "section_code", nullable = false, length = 32)
    private String sectionCode;

    @Column(name = "statement_order", nullable = false)
    private int statementOrder;

    @Column(name = "statement_text", nullable = false, columnDefinition = "TEXT")
    private String statementText;

    @Column(name = "critical", nullable = false)
    private boolean critical;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "ai_ambient_note_evidence",
            joinColumns = @JoinColumn(name = "note_statement_id"))
    @Column(name = "transcript_item_id", nullable = false)
    private Set<UUID> evidenceItemIds = new LinkedHashSet<>();

    protected AmbientNoteStatementEntity() {
    }

    public AmbientNoteStatementEntity(
            String sectionCode,
            int statementOrder,
            String statementText,
            boolean critical,
            Set<UUID> evidenceItemIds) {
        this.id = UUID.randomUUID();
        this.sectionCode = sectionCode;
        this.statementOrder = statementOrder;
        this.statementText = statementText;
        this.critical = critical;
        this.evidenceItemIds = new LinkedHashSet<>(evidenceItemIds);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    void attachTo(AmbientNoteRevisionEntity revision) {
        this.noteRevision = revision;
    }

    public UUID getId() { return id; }
    public String getSectionCode() { return sectionCode; }
    public int getStatementOrder() { return statementOrder; }
    public String getStatementText() { return statementText; }
    public boolean isCritical() { return critical; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<UUID> getEvidenceItemIds() { return Set.copyOf(evidenceItemIds); }
}
