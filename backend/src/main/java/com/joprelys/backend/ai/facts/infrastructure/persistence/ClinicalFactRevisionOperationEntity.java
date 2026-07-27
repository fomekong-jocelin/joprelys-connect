package com.joprelys.backend.ai.facts.infrastructure.persistence;

import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
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
@Table(name = "ai_clinical_fact_revision_operations")
public class ClinicalFactRevisionOperationEntity {

    @Id
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "visit_id", nullable = false)
    private UUID visitId;

    @Column(name = "operation_request_id", nullable = false)
    private UUID operationRequestId;

    @Column(name = "position_no", nullable = false)
    private int positionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 16)
    private OperationType operationType;

    @Column(name = "target_fact_id")
    private UUID targetFactId;

    @Column(name = "result_fact_id")
    private UUID resultFactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "retraction_reason", length = 32)
    private RetractionReason retractionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClinicalFactRevisionOperationEntity() {
    }

    public ClinicalFactRevisionOperationEntity(
            UUID batchId,
            UUID organizationId,
            UUID visitId,
            UUID operationRequestId,
            int positionNo,
            OperationType operationType,
            UUID targetFactId,
            UUID resultFactId,
            RetractionReason retractionReason) {
        this.id = UUID.randomUUID();
        this.batchId = batchId;
        this.organizationId = organizationId;
        this.visitId = visitId;
        this.operationRequestId = operationRequestId;
        this.positionNo = positionNo;
        this.operationType = operationType;
        this.targetFactId = targetFactId;
        this.resultFactId = resultFactId;
        this.retractionReason = retractionReason;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getVisitId() { return visitId; }
    public UUID getOperationRequestId() { return operationRequestId; }
    public int getPositionNo() { return positionNo; }
    public OperationType getOperationType() { return operationType; }
    public UUID getTargetFactId() { return targetFactId; }
    public UUID getResultFactId() { return resultFactId; }
    public RetractionReason getRetractionReason() { return retractionReason; }
    public Instant getCreatedAt() { return createdAt; }
}
