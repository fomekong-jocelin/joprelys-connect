package com.joprelys.backend.prescription.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Brute-force protection of the public prescription PIN (SEC-01 / FA-39).
 *
 * <p>Attempts are persisted on the prescription row and reserved atomically before the PIN is compared,
 * so parallel requests or a server restart cannot exceed {@link #MAX_FAILED_ATTEMPTS} guesses per lockout
 * window. Counters are written in their own transaction so that the failed request's rollback keeps them.
 * Unknown prescription numbers and wrong PINs receive the same answer, which prevents enumeration.
 */
@Service
public class PrescriptionPinGuard {

    static final int MAX_FAILED_ATTEMPTS = 3;
    static final Duration LOCKOUT = Duration.ofMinutes(15);
    private static final int PIN_DIGITS = 6;
    private static final String INVALID_CREDENTIALS = "Numéro d'ordonnance ou code PIN incorrect.";
    private static final String LOCKED =
            "Trop d'essais erronés : cette ordonnance est bloquée pendant 15 minutes.";

    private final SecureRandom random = new SecureRandom();
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate independentTransaction;
    private final Clock clock;

    public PrescriptionPinGuard(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.independentTransaction = new TransactionTemplate(transactionManager);
        this.independentTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    public String generatePin() {
        int bound = (int) Math.pow(10, PIN_DIGITS);
        return String.format(Locale.ROOT, "%0" + PIN_DIGITS + "d", random.nextInt(bound));
    }

    /**
     * Consumes one attempt for this prescription, then checks the PIN in constant time.
     *
     * @throws ResponseStatusException 401 for an unknown number or a wrong PIN, 429 while locked
     */
    public void verify(String prescriptionNumber, String storedPin, String providedPin) {
        reserveAttempt(prescriptionNumber);
        if (!pinMatches(storedPin, providedPin)) {
            recordFailure(prescriptionNumber);
        }
        resetAttempts(prescriptionNumber);
    }

    /** Test support: clears every counter and lock. */
    public void resetAll() {
        independentTransaction.executeWithoutResult(status ->
                jdbcTemplate.update("UPDATE prescriptions SET pin_failed_attempts = 0, pin_locked_until = NULL"));
    }

    private void reserveAttempt(String prescriptionNumber) {
        Timestamp now = Timestamp.from(clock.instant());
        Integer reserved = independentTransaction.execute(status -> jdbcTemplate.update("""
                UPDATE prescriptions SET pin_failed_attempts = pin_failed_attempts + 1
                WHERE prescription_number = ?
                  AND (pin_locked_until IS NULL OR pin_locked_until <= ?)
                  AND pin_failed_attempts < ?
                """, prescriptionNumber, now, MAX_FAILED_ATTEMPTS));
        if (reserved != null && reserved > 0) {
            return;
        }
        List<Timestamp> rows = jdbcTemplate.queryForList(
                "SELECT pin_locked_until FROM prescriptions WHERE prescription_number = ?",
                Timestamp.class, prescriptionNumber);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, LOCKED);
    }

    private void recordFailure(String prescriptionNumber) {
        Timestamp lockedUntil = Timestamp.from(clock.instant().plus(LOCKOUT));
        Integer locked = independentTransaction.execute(status -> jdbcTemplate.update("""
                UPDATE prescriptions SET pin_failed_attempts = 0, pin_locked_until = ?
                WHERE prescription_number = ? AND pin_failed_attempts >= ?
                """, lockedUntil, prescriptionNumber, MAX_FAILED_ATTEMPTS));
        if (locked != null && locked > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, LOCKED);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
    }

    private void resetAttempts(String prescriptionNumber) {
        independentTransaction.executeWithoutResult(status -> jdbcTemplate.update(
                "UPDATE prescriptions SET pin_failed_attempts = 0 WHERE prescription_number = ?",
                prescriptionNumber));
    }

    private static boolean pinMatches(String storedPin, String providedPin) {
        if (storedPin == null || providedPin == null) {
            return false;
        }
        return MessageDigest.isEqual(
                storedPin.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                providedPin.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
}
