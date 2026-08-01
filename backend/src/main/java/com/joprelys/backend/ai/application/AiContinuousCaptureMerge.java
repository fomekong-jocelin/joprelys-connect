package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic accumulator for already-grounded capture changes.
 *
 * <p>The model extracts only facts from the current chunk. This class combines
 * those safe deltas with the working draft so a later chunk cannot erase facts
 * captured earlier simply because a SET proposal replaces a field.</p>
 */
final class AiContinuousCaptureMerge {

    private static final Set<String> APPEND_TEXT_FIELDS = Set.of(
            "symptoms", "clinicalExam", "advice", "followUp");

    private final ObjectMapper objectMapper;

    AiContinuousCaptureMerge(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<ParsedChange> merge(Map<String, String> currentDraft, List<ParsedChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return List.of();
        }
        List<ParsedChange> merged = new ArrayList<>();
        for (ParsedChange change : changes) {
            if (change == null || !"SET".equals(change.operation()) || change.proposedValue() == null) {
                merged.add(change);
                continue;
            }
            String previous = currentDraft == null ? null : currentDraft.get(change.field());
            String next = switch (change.field()) {
                case "prescription" -> mergePrescription(previous, change.proposedValue());
                case "labOrders" -> mergeLabOrders(previous, change.proposedValue());
                case "vitals" -> mergeVitals(previous, change.proposedValue());
                default -> mergeText(change.field(), previous, change.proposedValue());
            };
            merged.add(new ParsedChange(
                    change.field(),
                    change.operation(),
                    next,
                    change.reason(),
                    change.uncertainty(),
                    change.evidence()));
        }
        return List.copyOf(merged);
    }

    private String mergeText(String field, String previous, String proposed) {
        String oldValue = clean(previous);
        String newValue = clean(proposed);
        if (oldValue.isBlank()) return newValue;
        if (newValue.isBlank()) return oldValue;

        String oldKey = normalize(oldValue);
        String newKey = normalize(newValue);
        if (oldKey.equals(newKey) || oldKey.contains(newKey)) return oldValue;
        if (newKey.contains(oldKey)) return newValue;

        if (!APPEND_TEXT_FIELDS.contains(field)) {
            // Diagnosis and conclusion are semantic decisions rather than additive
            // ledgers. A later explicit statement is allowed to supersede them.
            return newValue;
        }
        return oldValue + "\n" + newValue;
    }

    private String mergePrescription(String previous, String proposed) {
        try {
            List<Object> result = new ArrayList<>();
            Object oldValue = parse(previous);
            if (oldValue instanceof List<?> oldList) result.addAll(oldList);
            Object newValue = parse(proposed);
            if (!(newValue instanceof List<?> newList)) return proposed;
            for (Object item : newList) {
                if (!result.contains(item)) result.add(item);
            }
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            return proposed;
        }
    }

    private String mergeLabOrders(String previous, String proposed) {
        try {
            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            addLabOrders(result, parse(previous));
            addLabOrders(result, parse(proposed));
            return objectMapper.writeValueAsString(new ArrayList<>(result.values()));
        } catch (Exception exception) {
            return proposed;
        }
    }

    private void addLabOrders(Map<String, String> target, Object raw) {
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (item instanceof String value && !value.isBlank()) {
                target.putIfAbsent(normalize(value), value.trim());
            }
        }
    }

    private String mergeVitals(String previous, String proposed) {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            Object oldValue = parse(previous);
            if (oldValue instanceof Map<?, ?> oldMap) copyMap(result, oldMap);
            Object newValue = parse(proposed);
            if (!(newValue instanceof Map<?, ?> newMap)) return proposed;
            copyMap(result, newMap);
            return objectMapper.writeValueAsString(result);
        } catch (Exception exception) {
            return proposed;
        }
    }

    private void copyMap(Map<String, Object> target, Map<?, ?> source) {
        source.forEach((key, value) -> {
            if (key != null) target.put(key.toString(), value);
        });
    }

    private Object parse(String raw) throws Exception {
        return raw == null || raw.isBlank() ? null : objectMapper.readValue(raw, Object.class);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
