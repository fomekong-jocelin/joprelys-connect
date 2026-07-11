package com.joprelys.backend.emergency.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "emergency_admission_requests")
public class EmergencyAdmissionRequestEntity {

    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";

    @Id
    @Column(name = "request_id")
    private UUID requestId;

    @TenantId
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "emergency_id")
    private UUID emergencyId;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EmergencyAdmissionRequestEntity() {
    }

    public EmergencyAdmissionRequestEntity(UUID requestId, UUID organizationId) {
        Instant now = Instant.now();
        this.requestId = requestId;
        this.organizationId = organizationId;
        this.status = PROCESSING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void complete(UUID emergencyId) {
        this.emergencyId = emergencyId;
        this.status = COMPLETED;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getEmergencyId() {
        return emergencyId;
    }

    public String getStatus() {
        return status;
    }
}
