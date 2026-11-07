package com.joprelys.backend.eco.config;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'API écosystème (ECO-20261107, ADR-20261107-JOPRELYS-ID-KEYCLOAK).
 *
 * @param sourceTenant identifiant court de l'établissement émetteur, publié dans {@code meta.source}
 *                     sous la forme {@code joprelys:<sourceTenant>}
 * @param identity     fournisseur d'identité de liaison (Joprelys ID)
 * @param applications client OAuth ({@code azp}) → code de l'application de l'écosystème autorisée
 * @param webhooks     abonnés aux webhooks signés (code application → URL et secret) et réglages de l'outbox
 */
@ConfigurationProperties(prefix = "joprelys.eco")
public record EcoProperties(String sourceTenant, Identity identity, Map<String, String> applications, Webhooks webhooks) {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    public EcoProperties {
        if (sourceTenant == null || sourceTenant.isBlank() || identity == null) {
            throw new IllegalStateException(
                    "Configuration joprelys.eco incomplète : source-tenant et identity (issuer-uri, jwk-set-uri, audience) sont requis");
        }
        applications = applications == null ? Map.of() : Map.copyOf(applications);
        webhooks = webhooks == null ? new Webhooks(null, null) : webhooks;
    }

    /** Valeur de {@code meta.source} et de {@code issuer} dans les contrats. */
    public String source() {
        return "joprelys:" + sourceTenant;
    }

    public Optional<String> applicationFor(String clientId) {
        return Optional.ofNullable(clientId).map(applications::get);
    }

    public record Identity(String issuerUri, String jwkSetUri, String audience) {
    }

    /**
     * @param subscribers abonnés actifs ; un abonné sans URL ni secret est ignoré (aucun événement mis en file)
     * @param outbox      rejeu : délai initial, délai maximal entre deux essais, durée de vie avant file d'erreurs
     */
    public record Webhooks(Map<String, Subscriber> subscribers, Outbox outbox) {

        public Webhooks {
            subscribers = subscribers == null ? Map.of() : Map.copyOf(subscribers);
            subscribers.forEach((application, subscriber) -> subscriber.validate(application));
            outbox = outbox == null ? new Outbox(null, null, null, 0) : outbox;
        }

        public Optional<Subscriber> subscriber(String application) {
            return Optional.ofNullable(subscribers.get(application)).filter(Subscriber::enabled);
        }
    }

    public record Subscriber(String url, String secret) {

        static final int MIN_SECRET_LENGTH = 32;

        public boolean enabled() {
            return url != null && !url.isBlank() && secret != null && !secret.isBlank();
        }

        void validate(String application) {
            if (!enabled()) {
                return;
            }
            if (secret.length() < MIN_SECRET_LENGTH) {
                throw new IllegalStateException("Secret de webhook trop court pour l'abonné " + application
                        + " (" + MIN_SECRET_LENGTH + " caractères minimum)");
            }
            URI uri = URI.create(url);
            boolean local = uri.getHost() != null && LOCAL_HOSTS.contains(uri.getHost());
            if (!"https".equals(uri.getScheme()) && !local) {
                throw new IllegalStateException("URL de webhook non HTTPS pour l'abonné " + application);
            }
        }
    }

    public record Outbox(Duration initialDelay, Duration maxDelay, Duration maxAge, int batchSize) {

        public Outbox {
            initialDelay = initialDelay == null ? Duration.ofSeconds(30) : initialDelay;
            maxDelay = maxDelay == null ? Duration.ofHours(1) : maxDelay;
            maxAge = maxAge == null ? Duration.ofHours(24) : maxAge;
            batchSize = batchSize <= 0 ? 50 : batchSize;
        }
    }
}
