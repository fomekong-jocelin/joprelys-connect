package com.joprelys.backend.eco.infrastructure.webhook;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Déclenche le relais de l'outbox à intervalle fixe ({@code joprelys.eco.webhooks.outbox.relay-enabled}). */
@Component
@ConditionalOnProperty(name = "joprelys.eco.webhooks.outbox.relay-enabled", havingValue = "true")
public class EcoOutboxScheduler {

    private final EcoOutboxRelay relay;
    private final Clock clock;

    public EcoOutboxScheduler(EcoOutboxRelay relay, Clock clock) {
        this.relay = relay;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${joprelys.eco.webhooks.outbox.poll-interval:PT15S}")
    public void relay() {
        relay.relayDue(clock.instant());
    }
}
