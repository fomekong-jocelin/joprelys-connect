package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * current source and may not introduce unsupported clinical tokens. Structured
 * fields are sanitized leaf-by-leaf so one noisy ASR attribute cannot erase an
 * independently grounded medication, examination or vital sign.</p>
 */
final class AiClinicalFactualityGuard {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalFactualityGuard.class);
    private static final Set<String> STRUCTURED_FIELDS = Set.of(
            "prescription", "labOrders", "vitals");
    private static final Set<String> NEGATION_TOKENS = Set.of(
            "pas", "sans", "aucun", "aucune", "non", "nie", "negation",
            "not", "no", "without", "denies", "denied");
    private static final Set<String> CLEAR_INTENT_TOKENS = Set.of(
            "supprime", "supprimer", "efface", "effacer", "retire", "retirer",
            "enleve", "enlever", "annule", "annuler", "arrete", "arreter",
            "corrige", "corriger", "remplace", "remplacer", "delete", "remove",
            "clear", "cancel", "stop", "discontinue", "replace", "correct");
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
            ParsedChange safeChange = groundedChange(change, current, acceptedDraft);
            if (safeChange != null) {
                grounded.add(safeChange);
            } else {
                log.warn(
                        "Blocked ungrounded clinical proposal field={} source={}",
                        change == null ? "unknown" : change.field(),
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

    private ParsedChange groundedChange(
            ParsedChange change,
            String current,
            Map<String, String> acceptedDraft) {
        if (change == null || change.evidence() == null || change.evidence().isEmpty()) {
            return null;
        }
        String normalizedCurrent = normalize(current);
        for (String quote : change.evidence()) {
            String normalizedQuote = normalize(quote);
            if (normalizedQuote.isBlank() || !normalizedCurrent.contains(normalizedQuote)) {
                return null;
            }
        }

        if ("CLEAR".equals(change.operation())) {
            return hasExplicitClearIntent(normalizedCurrent) ? change : null;
        }
        String previous = acceptedDraft == null
                ? ""
                : acceptedDraft.getOrDefault(change.field(), "");
        String authorizedSource = previous + " " + current;
        if (STRUCTURED_FIELDS.contains(change.field())) {
            return sanitizeStructuredChange(change, authorizedSource);
        }
        return textValueSupported(change, authorizedSource) ? change : null;
    }

    private boolean hasExplicitClearIntent(String normalizedCurrent) {
        Set<String> tokens = significantTokens(normalizedCurrent);
        return CLEAR_INTENT_TOKENS.stream().anyMatch(tokens::contains);
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

    private ParsedChange sanitizeStructuredChange(ParsedChange change, String authorizedSource) {
        if (change.proposedValue() == null || change.proposedValue().isBlank()) {
            return null;
        }
        try {
            Object value = objectMapper.readValue(change.proposedValue(), Object.class);
            Object safeValue = switch (change.field()) {
                case "prescription" -> sanitizePrescription(value, authorizedSource);
                case "labOrders" -> sanitizeLabOrders(value, authorizedSource);
                case "vitals" -> sanitizeVitals(value, authorizedSource);
                default -> null;
            };
            if (safeValue == null) {
                return null;
            }
            String serialized = objectMapper.writeValueAsString(safeValue);
            return new ParsedChange(
                    change.field(),
                    change.operation(),
                    serialized,
                    change.reason(),
                    change.uncertainty(),
                    change.evidence());
        } catch (Exception exception) {
            return null;
        }
    }

    private List<Map<String, Object>> sanitizePrescription(Object value, String authorizedSource) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<Map<String, Object>> safeLines = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> line)) {
                continue;
            }
            Object rawDrugName = line.get("drugName");
            if (!(rawDrugName instanceof String drugName)
                    || !leafSupported(drugName, authorizedSource, false)) {
                continue;
            }
            Map<String, Object> safeLine = new LinkedHashMap<>();
            safeLine.put("drugName", drugName);
            for (Map.Entry<?, ?> entry : line.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toString();
                if ("drugName".equals(key) || "substitutionAllowed".equals(key)) {
                    continue;
                }
                Object itemValue = entry.getValue();
                if (itemValue != null && leafSupported(itemValue, authorizedSource, false)) {
                    safeLine.put(key, itemValue);
                }
            }
            safeLines.add(safeLine);
        }
        return safeLines.isEmpty() ? null : List.copyOf(safeLines);
    }

    private List<String> sanitizeLabOrders(Object value, String authorizedSource) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        List<String> safeOrders = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String exam && leafSupported(exam, authorizedSource, false)) {
                safeOrders.add(exam);
            }
        }
        return safeOrders.isEmpty() ? null : List.copyOf(safeOrders);
    }

    private Map<String, Number> sanitizeVitals(Object value, String authorizedSource) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Number> safeVitals = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null
                    && entry.getValue() instanceof Number number
                    && leafSupported(number, authorizedSource, true)) {
                safeVitals.put(entry.getKey().toString(), number);
            }
        }
        return safeVitals.isEmpty() ? null : Map.copyOf(safeVitals);
    }

    private boolean leafSupported(Object value, String authorizedSource, boolean numericOnly) {
        if (value == null || value instanceof Boolean) {
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
                // Harmless ASR typography normalization: 1000mg == 1000 mg and
                // 1cuillere == 1 cuillere. This changes formatting, never meaning.
                .replaceAll("(?<=\\d)(?=[a-z])|(?<=[a-z])(?=\\d)", " ")
                .replaceAll("[^a-z0-9.,]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
