package com.joprelys.backend.eco.infrastructure.webhook;

import com.joprelys.backend.eco.config.EcoProperties;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * Relais de l'outbox : envoie les événements dus, hors de toute transaction base de données, puis enregistre
 * le résultat ligne par ligne. Livraison « au moins une fois » : le récepteur déduplique sur Idempotency-Key.
 */
@Component
public class EcoOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(EcoOutboxRelay.class);

    private final EcoOutboxRepository outboxRepository;
    private final EcoWebhookSender sender;
    private final EcoProperties properties;
    private final EcoRetryPolicy retryPolicy;

    public EcoOutboxRelay(EcoOutboxRepository outboxRepository, EcoWebhookSender sender, EcoProperties properties) {
        this.outboxRepository = outboxRepository;
        this.sender = sender;
        this.properties = properties;
        this.retryPolicy = new EcoRetryPolicy(properties.webhooks().outbox());
    }

    /** @return nombre d'événements traités (livrés, reprogrammés ou abandonnés) */
    public int relayDue(Instant now) {
        List<EcoOutboxEntity> due = outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                EcoOutboxEntity.Status.PENDING, now, Limit.of(properties.webhooks().outbox().batchSize()));
        due.forEach(entry -> outboxRepository.save(deliver(entry, now)));
        return due.size();
    }

    private EcoOutboxEntity deliver(EcoOutboxEntity entry, Instant now) {
        EcoWebhookSender.DeliveryResult result = properties.webhooks().subscriber(entry.getSubscriber())
                .map(subscriber -> sender.send(subscriber, entry.getEventId(), entry.getPayload()))
                .orElseGet(() -> EcoWebhookSender.DeliveryResult.failed("subscriber not configured"));
        if (result.delivered()) {
            entry.markDelivered(now);
            return entry;
        }
        Optional<Instant> next = retryPolicy.nextAttempt(entry.getAttempts() + 1, entry.getCreatedAt(), now);
        if (next.isPresent()) {
            entry.scheduleRetry(next.get(), result.error());
        } else {
            entry.markDead(result.error());
            // Alerte d'exploitation : identifiants techniques uniquement, jamais le contenu de l'événement.
            log.error("Webhook écosystème abandonné (file d'erreurs) : outboxId={} eventId={} type={} abonné={} "
                            + "tentatives={} dernièreErreur={}", entry.getId(), entry.getEventId(),
                    entry.getEventType(), entry.getSubscriber(), entry.getAttempts(), entry.getLastError());
        }
        return entry;
    }
}
