package com.joprelys.backend.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Défis OTP à usage unique, en mémoire et sûrs en concurrence (SEC-05).
 *
 * <ul>
 *   <li>comparaison du code en temps constant ({@link MessageDigest#isEqual}) ;</li>
 *   <li>chaque vérification est atomique par clé ({@link ConcurrentHashMap#compute}) : des
 *       requêtes parallèles ne peuvent ni dépasser le nombre d'essais autorisé, ni consommer
 *       deux fois un même code ;</li>
 *   <li>les entrées expirées sont purgées à chaque émission (mémoire bornée).</li>
 * </ul>
 *
 * <p>Avec un {@code attemptWindow} nul, l'atteinte du plafond d'essais détruit le défi et une
 * nouvelle émission repart de zéro. Avec un {@code attemptWindow} positif, les essais échoués
 * sont conservés d'une émission à l'autre pendant la fenêtre, et l'atteinte du plafond verrouille
 * la clé jusqu'à la fin de cette fenêtre (aucune remise à zéro par simple redemande, SEC-06).</p>
 */
public final class OtpChallengeStore {

    public enum Outcome { VERIFIED, NO_ACTIVE_CHALLENGE, EXPIRED, INCORRECT, TOO_MANY_ATTEMPTS }

    public record Verification(Outcome outcome, int remainingAttempts) {
        public boolean verified() {
            return outcome == Outcome.VERIFIED;
        }
    }

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration codeTtl;
    private final int maxAttempts;
    private final Duration attemptWindow;

    public OtpChallengeStore(Clock clock, Duration codeTtl, int maxAttempts, Duration attemptWindow) {
        if (maxAttempts < 1 || codeTtl.isNegative() || codeTtl.isZero() || attemptWindow.isNegative()) {
            throw new IllegalArgumentException("Politique OTP invalide.");
        }
        this.clock = Objects.requireNonNull(clock);
        this.codeTtl = codeTtl;
        this.maxAttempts = maxAttempts;
        this.attemptWindow = attemptWindow;
    }

    /**
     * Enregistre un nouveau code pour la clé.
     *
     * @return {@code false} si la clé est verrouillée (plafond d'essais atteint dans la fenêtre)
     */
    public boolean issue(String key, String code) {
        Instant now = clock.instant();
        purgeExpired(now);
        AtomicReference<Boolean> accepted = new AtomicReference<>(true);
        challenges.compute(key, (ignored, current) -> {
            if (current == null || !carriesAttempts() || current.windowOver(now, attemptWindow)) {
                return new Challenge(code, now, 0, now);
            }
            if (current.locked(maxAttempts)) {
                accepted.set(false);
                return current;
            }
            return new Challenge(code, now, current.failedAttempts(), current.windowStartedAt());
        });
        return accepted.get();
    }

    /** Vérifie et, en cas de succès, consomme le code. */
    public Verification verify(String key, String submittedCode) {
        Instant now = clock.instant();
        AtomicReference<Verification> result = new AtomicReference<>();
        challenges.compute(key, (ignored, current) -> {
            if (current == null) {
                result.set(new Verification(Outcome.NO_ACTIVE_CHALLENGE, 0));
                return null;
            }
            if (current.locked(maxAttempts) && current.windowOver(now, attemptWindow)) {
                result.set(new Verification(Outcome.NO_ACTIVE_CHALLENGE, 0));
                return null;
            }
            if (current.locked(maxAttempts)) {
                result.set(new Verification(Outcome.TOO_MANY_ATTEMPTS, 0));
                return current;
            }
            if (current.code() == null) {
                result.set(new Verification(Outcome.NO_ACTIVE_CHALLENGE, 0));
                return current;
            }
            if (current.codeExpired(now, codeTtl)) {
                result.set(new Verification(Outcome.EXPIRED, 0));
                return carriesAttempts() ? current.withoutCode() : null;
            }
            if (constantTimeEquals(current.code(), submittedCode)) {
                result.set(new Verification(Outcome.VERIFIED, 0));
                return null;
            }
            int failed = current.failedAttempts() + 1;
            if (failed >= maxAttempts) {
                result.set(new Verification(Outcome.TOO_MANY_ATTEMPTS, 0));
                return carriesAttempts() ? current.withFailures(failed).withoutCode() : null;
            }
            result.set(new Verification(Outcome.INCORRECT, maxAttempts - failed));
            return current.withFailures(failed);
        });
        return result.get();
    }

    /** Supprime totalement le défi, compteur d'essais compris. */
    public void invalidate(String key) {
        challenges.remove(key);
    }

    /**
     * Retire le code actif (par exemple non délivré) sans effacer les essais échoués lorsque
     * la politique les conserve ; sinon équivaut à {@link #invalidate(String)}.
     */
    public void discardCode(String key) {
        challenges.computeIfPresent(key, (ignored, current) -> carriesAttempts() ? current.withoutCode() : null);
    }

    /** Code actif d'une clé : réservé aux tests d'intégration (exposition OTP désactivée en production). */
    public Optional<String> activeCodeForTesting(String key) {
        return Optional.ofNullable(challenges.get(key)).map(Challenge::code);
    }

    int trackedKeys() {
        return challenges.size();
    }

    private boolean carriesAttempts() {
        return !attemptWindow.isZero();
    }

    private void purgeExpired(Instant now) {
        challenges.entrySet().removeIf(entry -> entry.getValue().retentionOver(now, codeTtl, attemptWindow));
    }

    private static boolean constantTimeEquals(String expected, String submitted) {
        if (submitted == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                submitted.getBytes(StandardCharsets.UTF_8));
    }

    private record Challenge(String code, Instant issuedAt, int failedAttempts, Instant windowStartedAt) {

        boolean locked(int maxAttempts) {
            return failedAttempts >= maxAttempts;
        }

        boolean codeExpired(Instant now, Duration codeTtl) {
            return issuedAt.plus(codeTtl).isBefore(now);
        }

        boolean windowOver(Instant now, Duration attemptWindow) {
            return windowStartedAt.plus(attemptWindow).isBefore(now);
        }

        boolean retentionOver(Instant now, Duration codeTtl, Duration attemptWindow) {
            return codeExpired(now, codeTtl) && windowOver(now, attemptWindow);
        }

        Challenge withFailures(int failed) {
            return new Challenge(code, issuedAt, failed, windowStartedAt);
        }

        Challenge withoutCode() {
            return new Challenge(null, issuedAt, failedAttempts, windowStartedAt);
        }
    }
}
