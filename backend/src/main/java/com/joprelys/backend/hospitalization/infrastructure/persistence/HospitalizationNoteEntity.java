package com.joprelys.backend.hospitalization.infrastructure.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hospitalization_notes")
public class HospitalizationNoteEntity {

    @Id
    private UUID id;

    @Column(name = "hospitalization_id", nullable = false)
    private UUID hospitalizationId;

    @TenantId
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "note_content", nullable = false)
    private String noteContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HospitalizationNoteEntity() {
    }

    public HospitalizationNoteEntity(UUID hospitalizationId, String authorName, String noteContent) {
        this.id = UUID.randomUUID();
        this.hospitalizationId = hospitalizationId;
        this.authorName = authorName;
        this.noteContent = noteContent;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHospitalizationId() { return hospitalizationId; }
    public UUID getOrganizationId() { return organizationId; }
    public String getAuthorName() { return authorName; }
    public String getNoteContent() { return noteContent; }
    public Instant getCreatedAt() { return createdAt; }
}
