package com.joprelys.backend.eco.infrastructure.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.eco.config.EcoProperties;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EcoRetryPolicyTest {

    private static final Instant CREATED = Instant.parse("2026-11-07T10:00:00Z");
    private final EcoRetryPolicy policy = new EcoRetryPolicy(new EcoProperties.Outbox(null, null, null, 0));

    @Test
    void delayDoublesFromThirtySecondsAndIsCappedAtOneHour() {
        assertThat(policy.delay(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(policy.delay(2)).isEqualTo(Duration.ofSeconds(60));
        assertThat(policy.delay(5)).isEqualTo(Duration.ofMinutes(8));
        assertThat(policy.delay(8)).isEqualTo(Duration.ofHours(1));
        assertThat(policy.delay(500)).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void schedulesNextAttemptWithinTheTwentyFourHourLifetime() {
        Instant now = CREATED.plusSeconds(30);
        assertThat(policy.nextAttempt(2, CREATED, now)).contains(now.plusSeconds(60));
    }

    @Test
    void givesUpWhenTheNextAttemptWouldExceedTwentyFourHours() {
        Instant now = CREATED.plus(Duration.ofHours(23)).plus(Duration.ofMinutes(30));
        assertThat(policy.nextAttempt(20, CREATED, now)).isEmpty();
        assertThat(policy.nextAttempt(20, CREATED, CREATED.plus(Duration.ofHours(23)))).isPresent();
    }
}
