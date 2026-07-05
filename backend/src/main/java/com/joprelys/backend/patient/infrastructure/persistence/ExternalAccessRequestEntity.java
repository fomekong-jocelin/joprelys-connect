package com.joprelys.backend.patient.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_access_requests")
public class ExternalAccessRequestEntity {

    @Id
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "requester_organization_id", nullable = false)
    private UUID requesterOrganizationId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "requested_duration_hours", nullable = false)
    private Integer requestedDurationHours;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // EN_ATTENTE, APPROUVEE, REFUSEE, EXPIREE

    // WT1 (SCOPES): Granular access scopes for external requests
    @Column(name = "scopes", length = 500)
    private String scopes = "medical_records,prescriptions,lab_results,allergies_history";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected ExternalAccessRequestEntity() {
    }

    public ExternalAccessRequestEntity(UUID patientId, UUID requesterUserId, UUID requesterOrganizationId, String reason, Integer requestedDurationHours) {
        this.id = UUID.randomUUID();
        this.patientId = patientId;
        this.requesterUserId = requesterUserId;
        this.requesterOrganizationId = requesterOrganizationId;
        this.reason = reason;
        this.requestedDurationHours = requestedDurationHours;
        this.status = "EN_ATTENTE";
    }

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public UUID getPatientId() { return patientId; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public UUID getRequesterOrganizationId() { return requesterOrganizationId; }
    public String getReason() { return reason; }
    public Integer getRequestedDurationHours() { return requestedDurationHours; }
    public String getStatus() { return status; }
    // WT1 (SCOPES): Getter/Setter for granular scopes
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public void setStatus(String status) { this.status = status; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    // WT3 (DUPLICATES): Setter for patient reassignment during merge
    public void setPatientId(UUID patientId) { this.patientId = patientId; }
}
