package com.joprelys.backend.eco.infrastructure.webhook;

import com.joprelys.backend.eco.application.event.ConsentRevokedEvent;
import com.joprelys.backend.eco.application.event.EcoEventPublisher;
import com.joprelys.backend.eco.config.EcoProperties;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxEntity;
import com.joprelys.backend.eco.infrastructure.persistence.EcoOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Écrit chaque événement dans l'outbox, dans la transaction du changement métier (aucun envoi réseau ici). */
@Component
public class OutboxEcoEventPublisher implements EcoEventPublisher {

    static final String CONSENT_REVOKED = "consent.revoked";

    private static final Logger log = LoggerFactory.getLogger(OutboxEcoEventPublisher.class);

    private final EcoOutboxRepository outboxRepository;
    private final EcoProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxEcoEventPublisher(EcoOutboxRepository outboxRepository, EcoProperties properties,
                                   ObjectMapper objectMapper, Clock clock) {
        this.outboxRepository = outboxRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishConsentRevoked(ConsentRevokedEvent event) {
        if (properties.webhooks().subscriber(event.application()).isEmpty()) {
            // Sans abonné, l'application ne purgera pas sa copie : à corriger en configuration, jamais en silence.
            log.warn("Aucun abonné webhook configuré pour l'application {} : événement {} non mis en file",
                    event.application(), CONSENT_REVOKED);
            return;
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        UUID eventId = UUID.randomUUID();
        EcoEventEnvelope envelope = new EcoEventEnvelope(
                EcoEventEnvelope.SPEC_VERSION,
                eventId,
                CONSENT_REVOKED,
                properties.source(),
                iso(now),
                "Consent/" + event.consentId(),
                EcoEventEnvelope.SCHEMA_BASE + "consent-revoked.data.schema.json",
                new EcoEventEnvelope.ConsentRevokedData(event.consentId(), event.patientLinkId(),
                        iso(event.revokedAt()), event.revokedBy().contractValue()));
        outboxRepository.save(new EcoOutboxEntity(eventId, CONSENT_REVOKED, event.application(),
                objectMapper.writeValueAsString(envelope), now));
    }

    private static String iso(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }
}
