package com.joprelys.backend.common.api;

import com.joprelys.backend.common.infrastructure.persistence.WebhookEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Représentation API d'un webhook (SEC-20) : le secret de signature n'est restitué qu'une
 * seule fois, dans la réponse de création ; toutes les autres réponses le masquent.
 * Les noms de champs sont ceux de l'ancienne sérialisation de l'entité.
 */
public record WebhookResponse(
        UUID id,
        UUID organizationId,
        String url,
        String secret,
        String eventTypes,
        String status,
        Instant createdAt,
        Instant lastDeliveredAt,
        Integer failureCount) {

    static final String MASKED_SECRET = "********";

    /** Réponse de création : seule occasion de communiquer le secret à l'administrateur. */
    public static WebhookResponse revealingSecret(WebhookEntity webhook) {
        return from(webhook, webhook.getSecret());
    }

    public static WebhookResponse masked(WebhookEntity webhook) {
        return from(webhook, MASKED_SECRET);
    }

    private static WebhookResponse from(WebhookEntity webhook, String secret) {
        return new WebhookResponse(
                webhook.getId(),
                webhook.getOrganizationId(),
                webhook.getUrl(),
                secret,
                webhook.getEventTypes(),
                webhook.getStatus(),
                webhook.getCreatedAt(),
                webhook.getLastDeliveredAt(),
                webhook.getFailureCount());
    }
}
