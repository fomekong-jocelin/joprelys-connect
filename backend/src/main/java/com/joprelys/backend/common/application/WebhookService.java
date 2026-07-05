package com.joprelys.backend.common.application;

import com.joprelys.backend.common.infrastructure.persistence.WebhookEntity;
import com.joprelys.backend.common.infrastructure.persistence.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Service de gestion des webhooks.
 * Pour le pilote : CRUD + déclenchement simulé (log) sans appel HTTP réel.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final WebhookRepository webhookRepository;

    public WebhookService(WebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Transactional
    public WebhookEntity createWebhook(UUID organizationId, String url, String secret, String eventTypes) {
        if (url == null || url.isBlank() || !url.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'URL du webhook doit être une URL HTTPS valide.");
        }
        var webhook = new WebhookEntity(organizationId, url, secret, eventTypes);
        return webhookRepository.save(webhook);
    }

    @Transactional(readOnly = true)
    public List<WebhookEntity> getOrganizationWebhooks(UUID organizationId) {
        return webhookRepository.findByOrganizationIdAndStatus(organizationId, "ACTIVE");
    }

    @Transactional(readOnly = true)
    public List<WebhookEntity> getAllActiveWebhooks() {
        return webhookRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public WebhookEntity updateWebhook(UUID webhookId, UUID organizationId, String url, String secret, String eventTypes) {
        var webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook introuvable."));
        if (!webhook.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce webhook ne vous appartient pas.");
        }
        if (url != null && !url.isBlank()) {
            if (!url.startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'URL du webhook doit être une URL HTTPS valide.");
            }
            webhook.setUrl(url);
        }
        if (secret != null) webhook.setSecret(secret);
        if (eventTypes != null) webhook.setEventTypes(eventTypes);
        return webhookRepository.save(webhook);
    }

    @Transactional
    public void deleteWebhook(UUID webhookId, UUID organizationId) {
        var webhook = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Webhook introuvable."));
        if (!webhook.getOrganizationId().equals(organizationId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce webhook ne vous appartient pas.");
        }
        webhook.setStatus("INACTIVE");
        webhookRepository.save(webhook);
    }

    /**
     * Simule l'envoi d'un événement webhook.
     * Pour le pilote : log uniquement, pas d'appel HTTP réel.
     */
    public void triggerEvent(String eventType, UUID organizationId, String payload) {
        var webhooks = webhookRepository.findByOrganizationIdAndStatus(organizationId, "ACTIVE");
        for (var webhook : webhooks) {
            if (webhook.getEventTypes().contains(eventType)) {
                log.info("[WEBHOOK MOCK] Event: {} | URL: {} | Payload: {}", eventType, webhook.getUrl(), payload);
            }
        }
    }
}
