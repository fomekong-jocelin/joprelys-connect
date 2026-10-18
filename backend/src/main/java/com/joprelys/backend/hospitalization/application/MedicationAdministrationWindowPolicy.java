package com.joprelys.backend.hospitalization.application;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Fenêtre de sécurité entre deux administrations d'une même ligne de prescription (ADR-20261017 D-03).
 *
 * <p>La fréquence reste saisie en texte libre : l'intervalle n'est déduit que des formes non ambiguës
 * (« toutes les 8 h », « /24h », « q6h », « 3 fois par jour », « x2/j »). À défaut, la fenêtre vaut une heure.</p>
 */
@Component
public class MedicationAdministrationWindowPolicy {

    static final Duration DEFAULT_WINDOW = Duration.ofHours(1);
    static final Duration MINIMUM_WINDOW = Duration.ofMinutes(30);
    private static final Duration TOLERANCE = Duration.ofHours(1);

    private static final Pattern EVERY_HOURS = Pattern.compile(
            "(?:toutes\\s+les|every|q|/)\\s*(\\d{1,2})\\s*(?:h\\b|heures?|hours?)");
    private static final Pattern TIMES_PER_DAY = Pattern.compile(
            "(?:(\\d)\\s*(?:fois|x)\\s*(?:/|par)\\s*(?:j\\b|jour|day))|(?:x\\s*(\\d)\\s*/\\s*(?:j\\b|jour|day))");

    public Duration window(String frequency, String posology) {
        return interval(frequency).or(() -> interval(posology))
                .map(interval -> max(interval.minus(TOLERANCE), MINIMUM_WINDOW))
                .orElse(DEFAULT_WINDOW);
    }

    Optional<Duration> interval(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        Matcher hours = EVERY_HOURS.matcher(normalized);
        if (hours.find()) {
            int value = Integer.parseInt(hours.group(1));
            return value > 0 ? Optional.of(Duration.ofHours(value)) : Optional.empty();
        }
        Matcher perDay = TIMES_PER_DAY.matcher(normalized);
        if (perDay.find()) {
            int times = Integer.parseInt(perDay.group(1) != null ? perDay.group(1) : perDay.group(2));
            return times > 0 ? Optional.of(Duration.ofMinutes(24 * 60 / times)) : Optional.empty();
        }
        return Optional.empty();
    }

    private static Duration max(Duration left, Duration right) {
        return left.compareTo(right) >= 0 ? left : right;
    }
}
