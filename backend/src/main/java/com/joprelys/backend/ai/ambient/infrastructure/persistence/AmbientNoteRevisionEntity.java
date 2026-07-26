package com.joprelys.backend.ai.ambient.infrastructure.persistence;

import com.joprelys.backend.ai.ambient.domain.AmbientNoteStatus;
import com.joprelys.backend.ai.ambient.domain.AmbientNoteTemplate;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "ai_ambient_note_revisions")
public class AmbientNoteRevisionEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "revision_no", nullable = false)
    private long revisionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 32)
    private AmbientNoteTemplate templateCode;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AmbientNoteStatus status;

    @Column(name = "transcript_max_sequence", nullable = false)
    private long transcriptMaxSequence;

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "generated_by_user_id", nullable = false)
    private UUID generatedByUserId;

    @Column(name = "supersedes_note_id")
    private UUID supersedesNoteId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;

    @OneToMany(mappedBy = "noteRevision", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sectionCode ASC, statementOrder ASC")
    private List<AmbientNoteStatementEntity> statements = new ArrayList<>();

    protected AmbientNoteRevisionEntity() {
    }

    public AmbientNoteRevisionEntity(
            UUID organizationId,
            UUID visitId,
            long revisionNo,
            AmbientNoteTemplate templateCode,
            String locale,
            long transcriptMaxSequence,
            String modelName,
            Integer tokensUsed,
            UUID generatedByUserId,
            UUID supersedesNoteId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.revisionNo = revisionNo;
        this.templateCode = templateCode;
        this.locale = locale;
        this.status = AmbientNoteStatus.GENERATED;
        this.transcriptMaxSequence = transcriptMaxSequence;
        this.modelName = modelName;
        this.tokensUsed = tokensUsed;
        this.generatedByUserId = generatedByUserId;
        this.supersedesNoteId = supersedesNoteId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void addStatement(AmbientNoteStatementEntity statement) {
        statement.attachTo(this);
        statements.add(statement);
    }

    public void decide(AmbientNoteStatus decision, UUID userId, Instant now) {
        if (status != AmbientNoteStatus.GENERATED) {
            throw new IllegalStateException("AMBIENT_NOTE_ALREADY_DECIDED");
        }
        if (decision != AmbientNoteStatus.ACCEPTED && decision != AmbientNoteStatus.REJECTED) {
            throw new IllegalArgumentException("AMBIENT_NOTE_DECISION_INVALID");
        }
        status = decision;
        decidedByUserId = userId;
        decidedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public long getRevisionNo() { return revisionNo; }
    public AmbientNoteTemplate getTemplateCode() { return templateCode; }
    public String getLocale() { return locale; }
    public AmbientNoteStatus getStatus() { return status; }
    public long getTranscriptMaxSequence() { return transcriptMaxSequence; }
    public String getModelName() { return modelName; }
    public Integer getTokensUsed() { return tokensUsed; }
    public UUID getGeneratedByUserId() { return generatedByUserId; }
    public UUID getSupersedesNoteId() { return supersedesNoteId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public UUID getDecidedByUserId() { return decidedByUserId; }
    public List<AmbientNoteStatementEntity> getStatements() {
        return Collections.unmodifiableList(statements);
    }
}
