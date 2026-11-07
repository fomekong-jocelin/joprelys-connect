package com.joprelys.backend.eco.infrastructure.webhook;

import com.joprelys.backend.eco.config.EcoProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Rejeu exponentiel plafonné ; au-delà de la durée de vie de l'événement, il part en file d'erreurs. */
public final class EcoRetryPolicy {

    private final EcoProperties.Outbox settings;

    public EcoRetryPolicy(EcoProperties.Outbox settings) {
        this.settings = settings;
    }

    /**
     * @param failedAttempts nombre d'échecs, celui qui vient d'avoir lieu compris (≥ 1)
     * @return la date du prochain essai, ou vide si elle dépasserait la durée de vie de l'événement
     */
    public Optional<Instant> nextAttempt(int failedAttempts, Instant createdAt, Instant now) {
        Instant next = now.plus(delay(failedAttempts));
        return next.isAfter(createdAt.plus(settings.maxAge())) ? Optional.empty() : Optional.of(next);
    }

    Duration delay(int failedAttempts) {
        int exponent = Math.min(Math.max(failedAttempts - 1, 0), 30);
        Duration delay = settings.initialDelay().multipliedBy(1L << exponent);
        return delay.compareTo(settings.maxDelay()) > 0 ? settings.maxDelay() : delay;
    }
}
