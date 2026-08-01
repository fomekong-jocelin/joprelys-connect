package com.joprelys.backend.ai.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
final class AiClinicalResponseParser {

    static final Set<String> ALLOWED_FIELDS = Set.of(
            "symptoms", "clinicalExam", "diagnosis", "conclusion", "advice", "followUp",
            "prescription", "labOrders", "vitals");

    private static final Set<String> ROOT_FIELDS = Set.of(
            "changes", "assistantMessage", "needsClarification", "clarification");
    private static final Set<String> CHANGE_FIELDS = Set.of(
            "field", "operation", "value", "reason", "uncertainty", "evidence");
    private static final Set<String> CLARIFICATION_FIELDS = Set.of(
            "field", "question", "options");
    private static final Set<String> STRUCTURED_FIELDS = Set.of(
            "prescription", "labOrders", "vitals");
    private static final Set<String> PRESCRIPTION_FIELDS = Set.of(
            "drugName", "dosage", "posology", "duration", "quantity",
            "instructions", "form", "route", "frequency", "substitutionAllowed");
    private static final Set<String> ALLOWED_OPERATIONS = Set.of("SET", "CLEAR");
    private static final Set<String> ALLOWED_UNCERTAINTIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final int MAX_CLARIFICATION_OPTIONS = 5;
    private static final int MAX_EVIDENCE_ITEMS = 4;

    private final ObjectMapper objectMapper;

    AiClinicalResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedResponse parse(String content) {
        return parse(content, false);
    }

    /**
     * Parse a clinician-reviewed capture. The root contract remains strict, while one
     * malformed field proposal cannot erase independent, grounded changes from the
     * same transcript.
     */
    ParsedResponse parseCapture(String content) {
        return parse(content, true);
    }

    @SuppressWarnings("unchecked")
    private ParsedResponse parse(String content, boolean tolerateInvalidChanges) {
        if (content == null || content.isBlank()) {
            throw invalidOutput();
        }
        String cleaned = content.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
        try {
            Map<String, Object> root = objectMapper.readValue(cleaned, Map.class);
            rejectUnknownKeys(root, ROOT_FIELDS);
            if (!(root.get("changes") instanceof List<?>)) {
                throw invalidOutput();
            }
            String assistantMessage = root.get("assistantMessage") instanceof String value
                    && !value.isBlank()
                    ? limit(value.trim(), 2000)
                    : "";
            boolean needsClarification = root.get("needsClarification") instanceof Boolean value
                    && value;
            ParsedClarification clarification = needsClarification
                    ? parseClarification(root.get("clarification"))
                    : null;
            List<ParsedChange> changes = parseChanges(
                    root.get("changes"), tolerateInvalidChanges);
            if (needsClarification && clarification != null) {
                changes = changes.stream()
                        .filter(change -> !change.field().equals(clarification.field()))
                        .toList();
            }
            return new ParsedResponse(
                    changes,
                    assistantMessage,
                    needsClarification,
                    clarification);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidOutput();
        }
    }

    private List<ParsedChange> parseChanges(
            Object changesValue,
            boolean tolerateInvalidChanges) {
        if (!(changesValue instanceof List<?> changes)) {
            throw invalidOutput();
        }
        List<ParsedChange> result = new ArrayList<>();
        for (Object value : changes) {
            try {
                result.add(parseChange(value));
            } catch (ResponseStatusException exception) {
                if (!tolerateInvalidChanges
                        || !"AI_CHANGE_INVALID".equals(exception.getReason())) {
                    throw exception;
                }
            }
        }
        return List.copyOf(result);
    }

    private ParsedChange parseChange(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalidChange();
        }
        rejectUnknownKeys(map, CHANGE_FIELDS);
        String field = stringValue(map.get("field"));
        String operation = stringValue(map.get("operation")).toUpperCase();
        String reason = stringValue(map.get("reason"));
        String uncertainty = stringValue(map.get("uncertainty")).toUpperCase();
        List<String> evidence = parseEvidence(map.get("evidence"));
        if (!ALLOWED_FIELDS.contains(field)
                || !ALLOWED_OPERATIONS.contains(operation)
                || reason.isBlank()
                || !ALLOWED_UNCERTAINTIES.contains(uncertainty)
                || evidence.isEmpty()) {
            throw invalidChange();
        }
        String proposedValue = null;
        if ("SET".equals(operation)) {
            Object rawValue = map.get("value");
            proposedValue = STRUCTURED_FIELDS.contains(field)
                    ? normalizeStructuredFieldValue(field, rawValue)
                    : normalizeTextFieldValue(field, rawValue);
        }
        return new ParsedChange(
                field,
                operation,
                proposedValue,
                limit(reason, 1000),
                uncertainty,
                evidence);
    }

    private List<String> parseEvidence(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw invalidChange();
        }
        List<String> evidence = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String quote) || quote.isBlank()) {
                throw invalidChange();
            }
            evidence.add(limit(quote.trim(), 500));
            if (evidence.size() >= MAX_EVIDENCE_ITEMS) {
                break;
            }
        }
        return List.copyOf(evidence);
    }

    private ParsedClarification parseClarification(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw invalidClarification();
        }
        rejectUnknownKeys(map, CLARIFICATION_FIELDS);
        String field = stringValue(map.get("field"));
        String question = stringValue(map.get("question"));
        if (!ALLOWED_FIELDS.contains(field) || question.isBlank()) {
            throw invalidClarification();
        }
        List<String> options = new ArrayList<>();
        Object optionValue = map.get("options");
        if (optionValue instanceof List<?> optionList) {
            optionList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(String::trim)
                    .filter(option -> !option.isBlank())
                    .limit(MAX_CLARIFICATION_OPTIONS)
                    .map(option -> limit(option, 200))
                    .forEach(options::add);
        }
        return new ParsedClarification(
                field,
                limit(question, 1000),
                List.copyOf(options));
    }

    private String normalizeTextFieldValue(String field, Object value) {
        String text = stringValue(value);
        if (text.isBlank()) {
            throw invalidChange();
        }
        return limit(text, maximumFor(field));
    }

    private String normalizeStructuredFieldValue(String field, Object value) {
        if (value == null) {
            throw invalidChange();
        }
        try {
            Object parsed = value instanceof String text
                    ? objectMapper.readValue(text, Object.class)
                    : value;
            Object normalized = switch (field) {
                case "prescription" -> validatePrescription(parsed);
                case "labOrders" -> validateLabOrders(parsed);
                case "vitals" -> validateVitals(parsed);
                default -> throw invalidChange();
            };
            return limit(objectMapper.writeValueAsString(normalized), maximumFor(field));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidChange();
        }
    }

    private List<Map<String, Object>> validatePrescription(Object parsed) {
        if (!(parsed instanceof List<?> list)) {
            throw invalidChange();
        }
        List<Map<String, Object>> normalized = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw invalidChange();
            }
            Map<String, Object> line = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().toString();
                if (!PRESCRIPTION_FIELDS.contains(key)) {
                    throw invalidChange();
                }
                Object itemValue = entry.getValue();
                if ("substitutionAllowed".equals(key)) {
                    if (!(itemValue instanceof Boolean)) {
                        throw invalidChange();
                    }
                    line.put(key, itemValue);
                } else if (itemValue instanceof String text && !text.isBlank()) {
                    line.put(key, limit(text.trim(), 500));
                } else if (itemValue != null) {
                    throw invalidChange();
                }
            }
            Object drugName = line.get("drugName");
            if (!(drugName instanceof String name) || name.isBlank()) {
                throw invalidChange();
            }
            normalized.add(line);
        }
        return List.copyOf(normalized);
    }

    private List<String> validateLabOrders(Object parsed) {
        if (!(parsed instanceof List<?> list)) {
            throw invalidChange();
        }
        List<String> normalized = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String exam) || exam.isBlank()) {
                throw invalidChange();
            }
            normalized.add(limit(exam.trim(), 300));
        }
        return List.copyOf(normalized);
    }

    private Map<String, Number> validateVitals(Object parsed) {
        if (!(parsed instanceof Map<?, ?> map)) {
            throw invalidChange();
        }
        Map<String, Number> candidates = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toString();
            if (!AiVitalsSafetyRules.VITAL_FIELDS.contains(key)
                    || !(entry.getValue() instanceof Number number)) {
                throw invalidChange();
            }
            candidates.put(key, number);
        }
        normalizeBloodPressureShorthand(candidates);

        Map<String, Number> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Number> entry : candidates.entrySet()) {
            if (!AiVitalsSafetyRules.isSafeProposal(entry.getKey(), entry.getValue())) {
                throw invalidChange();
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        if (normalized.isEmpty()) {
            throw invalidChange();
        }
        return normalized;
    }

    private void normalizeBloodPressureShorthand(Map<String, Number> values) {
        Number systolic = values.get("systolic");
        Number diastolic = values.get("diastolic");
        if (systolic == null || diastolic == null) return;
        double systolicValue = systolic.doubleValue();
        double diastolicValue = diastolic.doubleValue();
        if (systolicValue >= 7.0d
                && systolicValue <= 25.0d
                && diastolicValue >= 4.0d
                && diastolicValue <= 15.0d) {
            values.put("systolic", scalePressure(systolicValue));
            values.put("diastolic", scalePressure(diastolicValue));
        }
    }

    private Number scalePressure(double value) {
        double scaled = value * 10.0d;
        return Math.rint(scaled) == scaled ? (int) scaled : scaled;
    }

    private void rejectUnknownKeys(Map<?, ?> map, Set<String> allowed) {
        for (Object key : map.keySet()) {
            if (key == null || !allowed.contains(key.toString())) {
                throw invalidOutput();
            }
        }
    }

    private int maximumFor(String field) {
        if (STRUCTURED_FIELDS.contains(field)) {
            return 12000;
        }
        return field.equals("followUp") ? 1000 : 5000;
    }

    private String stringValue(Object value) {
        return value instanceof String stringValue ? stringValue.trim() : "";
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private ResponseStatusException invalidOutput() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
    }

    private ResponseStatusException invalidChange() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CHANGE_INVALID");
    }

    private ResponseStatusException invalidClarification() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CLARIFICATION_INVALID");
    }

    record ParsedChange(
            String field,
            String operation,
            String proposedValue,
            String reason,
            String uncertainty,
            List<String> evidence) {

        ParsedChange(
                String field,
                String operation,
                String proposedValue,
                String reason,
                String uncertainty) {
            this(field, operation, proposedValue, reason, uncertainty, List.of());
        }
    }

    record ParsedClarification(
            String field,
            String question,
            List<String> options) {
    }

    record ParsedResponse(
            List<ParsedChange> changes,
            String assistantMessage,
            boolean needsClarification,
            ParsedClarification clarification) {
    }
}
