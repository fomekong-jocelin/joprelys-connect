package com.joprelys.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.auth.security.OtpChallengeStore.Outcome;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OtpChallengeStoreTest {

    private static final Duration TTL = Duration.ofMinutes(5);
    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-04T08:00:00Z"));

    @Test
    void correctCodeIsAcceptedOnceOnly() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ZERO);
        store.issue("k", "123456");

        assertThat(store.verify("k", "123456").outcome()).isEqualTo(Outcome.VERIFIED);
        assertThat(store.verify("k", "123456").outcome()).isEqualTo(Outcome.NO_ACTIVE_CHALLENGE);
    }

    @Test
    void wrongLengthNullAndNearMissCodesAreRejected() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 5, Duration.ZERO);
        store.issue("k", "123456");

        assertThat(store.verify("k", null).outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(store.verify("k", "12345").outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(store.verify("k", "1234567").outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(store.verify("k", "123457").remainingAttempts()).isEqualTo(1);
    }

    @Test
    void expiredCodeIsRejected() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ZERO);
        store.issue("k", "123456");
        clock.advance(TTL.plusSeconds(1));

        assertThat(store.verify("k", "123456").outcome()).isEqualTo(Outcome.EXPIRED);
    }

    @Test
    void withoutAttemptWindowTheChallengeIsDestroyedAtTheLimit() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ZERO);
        store.issue("k", "123456");

        assertThat(store.verify("k", "000000").outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(store.verify("k", "000000").outcome()).isEqualTo(Outcome.INCORRECT);
        assertThat(store.verify("k", "000000").outcome()).isEqualTo(Outcome.TOO_MANY_ATTEMPTS);
        assertThat(store.verify("k", "123456").outcome()).isEqualTo(Outcome.NO_ACTIVE_CHALLENGE);
        assertThat(store.issue("k", "654321")).isTrue();
    }

    @Test
    void withAttemptWindowReissuingDoesNotResetFailedAttempts() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ofMinutes(15));
        store.issue("k", "111111");
        store.verify("k", "000000");
        store.verify("k", "000000");

        assertThat(store.issue("k", "222222")).isTrue();
        assertThat(store.verify("k", "000000").outcome()).isEqualTo(Outcome.TOO_MANY_ATTEMPTS);
        assertThat(store.issue("k", "333333")).as("clé verrouillée pendant la fenêtre").isFalse();
        assertThat(store.verify("k", "333333").outcome()).isEqualTo(Outcome.TOO_MANY_ATTEMPTS);

        clock.advance(Duration.ofMinutes(16));
        assertThat(store.issue("k", "444444")).isTrue();
        assertThat(store.verify("k", "444444").outcome()).isEqualTo(Outcome.VERIFIED);
    }

    @Test
    void parallelWrongGuessesNeverExceedTheAttemptLimit() throws Exception {
        int maxAttempts = 3;
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, maxAttempts, Duration.ofMinutes(15));
        store.issue("k", "123456");
        int threads = 32;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                String guess = String.format("%06d", 900000 + i);
                results.add(executor.submit(() -> {
                    start.await();
                    return store.verify("k", guess).outcome();
                }));
            }
            start.countDown();
            long evaluatedGuesses = 0;
            for (Future<Outcome> result : results) {
                Outcome outcome = result.get(10, TimeUnit.SECONDS);
                if (outcome == Outcome.INCORRECT) {
                    evaluatedGuesses++;
                }
            }
            assertThat(evaluatedGuesses).isEqualTo(maxAttempts - 1);
            assertThat(store.verify("k", "123456").outcome()).isEqualTo(Outcome.TOO_MANY_ATTEMPTS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void parallelSubmissionsOfTheRightCodeSucceedOnlyOnce() throws Exception {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ZERO);
        store.issue("k", "123456");
        int threads = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return store.verify("k", "123456").outcome();
                }));
            }
            start.countDown();
            long successes = 0;
            for (Future<Outcome> result : results) {
                if (result.get(10, TimeUnit.SECONDS) == Outcome.VERIFIED) {
                    successes++;
                }
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void expiredChallengesArePurgedOnIssue() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ZERO);
        for (int i = 0; i < 50; i++) {
            store.issue("k" + i, "123456");
        }
        clock.advance(TTL.plusSeconds(1));
        store.issue("fresh", "123456");

        assertThat(store.trackedKeys()).isEqualTo(1);
    }

    @Test
    void discardedCodeKeepsFailedAttemptsWhenTheyAreCarried() {
        OtpChallengeStore store = new OtpChallengeStore(clock, TTL, 3, Duration.ofMinutes(15));
        store.issue("k", "111111");
        store.verify("k", "000000");
        store.verify("k", "000000");
        store.discardCode("k");

        assertThat(store.verify("k", "111111").outcome()).isEqualTo(Outcome.NO_ACTIVE_CHALLENGE);
        store.issue("k", "222222");
        assertThat(store.verify("k", "000000").outcome()).isEqualTo(Outcome.TOO_MANY_ATTEMPTS);
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
