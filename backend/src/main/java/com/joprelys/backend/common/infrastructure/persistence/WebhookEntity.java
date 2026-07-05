package com.joprelys.backend.common.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entité représentant un webhook enregistré par une organisation partenaire.
 * Le webhook est appelé lors d'événements métier (nouveau résultat labo, ordonnance créée, etc.).
 */
@Entity
@Table(name = "webhooks")
public class WebhookEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "secret", nullable = false, length = 200)
    private String secret;

    @Column(name = "event_types", nullable = false, length = 300)
    private String eventTypes; // comma-separated: LAB_RESULT_AVAILABLE, PRESCRIPTION_CREATED, etc.

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    protected WebhookEntity() {
    }

    public WebhookEntity(UUID organizationId, String url, String secret, String eventTypes) {
        this.id = UUID.randomUUID();
        this.organizationId = organizationId;
        this.url = url;
        this.secret = secret;
        this.eventTypes = eventTypes;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.failureCount = 0;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getUrl() { return url; }
    public String getSecret() { return secret; }
    public String getEventTypes() { return eventTypes; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastDeliveredAt() { return lastDeliveredAt; }
    public Integer getFailureCount() { return failureCount; }

    public void setId(UUID id) { this.id = id; }
    public void setUrl(String url) { this.url = url; }
    public void setSecret(String secret) { this.secret = secret; }
    public void setEventTypes(String eventTypes) { this.eventTypes = eventTypes; }
    public void setStatus(String status) { this.status = status; }
    public void setLastDeliveredAt(Instant lastDeliveredAt) { this.lastDeliveredAt = lastDeliveredAt; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
    public void incrementFailureCount() { this.failureCount = (this.failureCount == null ? 0 : this.failureCount) + 1; }
}
