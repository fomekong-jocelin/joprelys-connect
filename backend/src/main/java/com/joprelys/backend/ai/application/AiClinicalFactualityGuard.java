package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic factuality boundary applied after parsing and before any
 * clinical proposal can be created.
 *
 * <p>LLM output is never accepted as evidence. A proposed change must cite the
 * current source verbatim and may not introduce any unsupported clinical token.
 * When in doubt the change is dropped (fail closed).</p>
 */
final class AiClinicalFactualityGuard {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalFactualityGuard.class);
    private static final Set<String> STRUCTURED_FIELDS = Set.of(
            "prescription", "labOrders", "vitals");
    private static final Set<String> NEGATION_TOKENS = Set.of(
            "pas", "sans", "aucun", "aucune", "non", "nie", "negation",
            "not", "no", "without", "denies", "denied");
    private static final Set<String> SAFE_GLUE_WORDS = Set.of(
            "patient", "patiente", "presente", "signale", "rapporte",
            "avec", "pour", "depuis", "dans", "chez", "une", "des", "les", "est",
            "sont", "avait", "avoir", "the", "and", "with", "from", "for", "has",
            "have", "reports", "reported", "presents", "presenting", "of", "to");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9]+(?:[.,][0-9]+)?");

    private final ObjectMapper objectMapper;

    AiClinicalFactualityGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedResponse enforce(
            ParsedResponse parsed,
            String latestSource,
            Map<String, String> acceptedDraft,
            String inputSource,
            String locale) {
        if (parsed == null) {
            return null;
        }
        String current = latestSource == null ? "" : latestSource.trim();
        List<ParsedChange> grounded = new ArrayList<>();
        for (ParsedChange change : parsed.changes()) {
            if (isGrounded(change, current, acceptedDraft)) {
                grounded.add(change);
            } else {
                log.warn(
                        "Blocked ungrounded clinical proposal field={} source={}",
                        change.field(),
                        inputSource);
            }
        }

        ParsedClarification clarification = parsed.needsClarification()
                ? safeClarification(parsed.clarification(), locale)
                : null;
        String assistantMessage = clarification != null
                ? clarification.question()
                : operationalMessage(grounded.size(), locale);
        return new ParsedResponse(
                List.copyOf(grounded),
                assistantMessage,
                clarification != null,
                clarification);
    }

    private boolean isGrounded(
            ParsedChange change,
            String current,
            Map<String, String> acceptedDraft) {
        if (change == null || change.evidence() == null || change.evidence().isEmpty()) {
            return false;
        }
        String normalizedCurrent = normalize(current);
        for (String quote : change.evidence()) {
            String normalizedQuote = normalize(quote);
            if (normalizedQuote.isBlank() || !normalizedCurrent.contains(normalizedQuote)) {
                return false;
            }
        }

        if ("CLEAR".equals(change.operation())) {
            return true;
        }
        String previous = acceptedDraft == null
                ? ""
                : acceptedDraft.getOrDefault(change.field(), "");
        String authorizedSource = previous + " " + current;
        if (STRUCTURED_FIELDS.contains(change.field())) {
            return structuredValueSupported(change, authorizedSource);
        }
        return textValueSupported(change, authorizedSource);
    }

    private boolean textValueSupported(ParsedChange change, String authorizedSource) {
        String proposed = change.proposedValue();
        if (proposed == null || proposed.isBlank()) {
            return false;
        }
        Set<String> sourceTokens = significantTokens(authorizedSource);
        Set<String> proposedTokens = significantTokens(proposed);
        if (proposedTokens.isEmpty()) {
            return false;
        }

        for (String required : criticalEvidenceTokens(change.evidence())) {
            if (!proposedTokens.contains(required)) {
                return false;
            }
        }

        for (String token : proposedTokens) {
            if (SAFE_GLUE_WORDS.contains(token)) {
                continue;
            }
            if (!sourceTokens.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private boolean structuredValueSupported(ParsedChange change, String authorizedSource) {
        if (change.proposedValue() == null || change.proposedValue().isBlank()) {
            return false;
        }
        try {
            Object value = objectMapper.readValue(change.proposedValue(), Object.class);
            return switch (change.field()) {
                case "vitals" -> valuesSupported(value, authorizedSource, true);
                case "labOrders", "prescription" -> valuesSupported(value, authorizedSource, false);
                default -> false;
            };
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean valuesSupported(Object value, String authorizedSource, boolean numericOnly) {
        if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                if (!leafSupported(item, authorizedSource, numericOnly)) {
                    return false;
                }
            }
            return !map.isEmpty();
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (!valuesSupported(item, authorizedSource, numericOnly)) {
                    return false;
                }
            }
            return !list.isEmpty();
        }
        return leafSupported(value, authorizedSource, numericOnly);
    }

    private boolean leafSupported(Object value, String authorizedSource, boolean numericOnly) {
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean) {
            return false;
        }
        String text = value.toString();
        Set<String> sourceTokens = significantTokens(authorizedSource);
        Set<String> valueTokens = significantTokens(text);
        if (valueTokens.isEmpty()) {
            return false;
        }
        if (numericOnly && value instanceof Number) {
            return valueTokens.stream().allMatch(sourceTokens::contains);
        }
        return valueTokens.stream()
                .filter(token -> !SAFE_GLUE_WORDS.contains(token))
                .allMatch(sourceTokens::contains);
    }

    private Set<String> criticalEvidenceTokens(List<String> evidence) {
        Set<String> result = new HashSet<>();
        for (String quote : evidence) {
            for (String token : significantTokens(quote)) {
                if (isNumeric(token) || NEGATION_TOKENS.contains(token)) {
                    result.add(token);
                }
            }
        }
        return result;
    }

    private Set<String> significantTokens(String value) {
        Set<String> result = new HashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(normalize(value));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.length() >= 3 || isNumeric(token) || NEGATION_TOKENS.contains(token)) {
                result.add(token);
            }
        }
        return result;
    }

    private boolean isNumeric(String token) {
        return token.matches("\\d+(?:[.,]\\d+)?");
    }

    private ParsedClarification safeClarification(ParsedClarification original, String locale) {
        if (original == null) {
            return null;
        }
        String question = "en".equalsIgnoreCase(locale)
                ? "I am not sufficiently confident about this clinical point. Could you repeat or clarify it?"
                : "Je n’ai pas une confiance suffisante sur ce point clinique. Pouvez-vous le répéter ou le préciser ?";
        return new ParsedClarification(original.field(), question, List.of());
    }

    private String operationalMessage(int groundedCount, String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return groundedCount == 0
                    ? "No sufficiently grounded clinical item was retained."
                    : groundedCount + " grounded clinical proposal(s) are ready for your review.";
        }
        return groundedCount == 0
                ? "Aucun élément clinique suffisamment certain n’a été retenu."
                : groundedCount + " proposition(s) clinique(s) fondée(s) sur votre dictée sont prêtes à être vérifiées.";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9.,]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
