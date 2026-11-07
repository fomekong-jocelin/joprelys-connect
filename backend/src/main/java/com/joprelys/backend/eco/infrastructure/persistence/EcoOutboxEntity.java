package com.joprelys.backend.eco.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Événement à livrer à un abonné de l'écosystème (outbox transactionnelle). */
@Entity
@Table(name = "eco_outbox")
public class EcoOutboxEntity {

    public enum Status { PENDING, DELIVERED, DEAD }

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "subscriber", nullable = false, length = 50)
    private String subscriber;

    /** Enveloppe JSON complète, octet pour octet celle qui sera signée et envoyée. */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "last_error", length = 200)
    private String lastError;

    protected EcoOutboxEntity() {
    }

    public EcoOutboxEntity(UUID eventId, String eventType, String subscriber, String payload, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.eventId = eventId;
        this.eventType = eventType;
        this.subscriber = subscriber;
        this.payload = payload;
        this.status = Status.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
    }

    public void markDelivered(Instant when) {
        attempts++;
        status = Status.DELIVERED;
        deliveredAt = when;
        lastError = null;
    }

    public void scheduleRetry(Instant next, String error) {
        attempts++;
        nextAttemptAt = next;
        lastError = truncate(error);
    }

    public void markDead(String error) {
        attempts++;
        status = Status.DEAD;
        lastError = truncate(error);
    }

    private static String truncate(String error) {
        return error == null || error.length() <= 200 ? error : error.substring(0, 200);
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getSubscriber() { return subscriber; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public String getLastError() { return lastError; }
}
