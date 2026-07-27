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
@Table(name = "ai_clinical_fact_revision_batches")
public class ClinicalFactRevisionBatchEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "revision_request_id", nullable = false)
    private UUID revisionRequestId;

    @Column(name = "base_projection_version", nullable = false, length = 128)
    private String baseProjectionVersion;

    @Column(name = "result_projection_version", nullable = false, length = 128)
    private String resultProjectionVersion;

    @Column(name = "request_sha256", nullable = false, length = 64)
    private String requestSha256;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClinicalFactRevisionBatchEntity() {
    }

    public ClinicalFactRevisionBatchEntity(
            UUID organizationId,
            UUID visitId,
            UUID revisionRequestId,
            String baseProjectionVersion,
            String requestSha256,
            UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.revisionRequestId = revisionRequestId;
        this.baseProjectionVersion = baseProjectionVersion;
        this.resultProjectionVersion = baseProjectionVersion;
        this.requestSha256 = requestSha256;
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public void complete(String resultProjectionVersion) {
        this.resultProjectionVersion = resultProjectionVersion;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public UUID getRevisionRequestId() { return revisionRequestId; }
    public String getBaseProjectionVersion() { return baseProjectionVersion; }
    public String getResultProjectionVersion() { return resultProjectionVersion; }
    public String getRequestSha256() { return requestSha256; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
}
