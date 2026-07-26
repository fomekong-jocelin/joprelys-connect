package com.joprelys.backend.ai.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiVitalsResponseParser {

    static final Set<String> VITAL_FIELDS = AiVitalsSafetyRules.VITAL_FIELDS;

    private static final Set<String> ROOT_FIELDS = Set.of(
            "assistantMessage", "needsConfirmation", "confirmationReason", "vitals");

    private final ObjectMapper objectMapper;

    public AiVitalsResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public ParsedVitals parse(String content) {
        if (content == null || content.isBlank()) {
            throw invalidOutput();
        }
        String cleaned = content.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
        try {
            Map<String, Object> root = objectMapper.readValue(cleaned, Map.class);
            for (String key : root.keySet()) {
                if (!ROOT_FIELDS.contains(key)) {
                    throw invalidOutput();
                }
            }

            String assistantMessage = stringValue(root.get("assistantMessage"));
            if (assistantMessage.isBlank()) {
                assistantMessage = "Please review the detected vital signs before saving.";
            }
            boolean needsConfirmation = root.get("needsConfirmation") instanceof Boolean value && value;
            String confirmationReason = stringValue(root.get("confirmationReason"));
            Map<String, Double> vitals = parseVitals(root.get("vitals"));

            if (needsConfirmation && confirmationReason.isBlank()) {
                throw invalidOutput();
            }
            if (!needsConfirmation && vitals.isEmpty()) {
                throw invalidOutput();
            }

            return new ParsedVitals(
                    Map.copyOf(vitals),
                    limit(assistantMessage.trim(), 1000),
                    needsConfirmation,
                    limit(confirmationReason.trim(), 1000));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidOutput();
        }
    }

    private Map<String, Double> parseVitals(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw invalidOutput();
        }
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toString();
            if (!(entry.getValue() instanceof Number number)
                    || !AiVitalsSafetyRules.isSafeProposal(key, number)) {
                throw invalidOutput();
            }
            normalized.put(key, number.doubleValue());
        }
        return normalized;
    }

    private String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private ResponseStatusException invalidOutput() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
    }

    public record ParsedVitals(
            Map<String, Double> vitals,
            String assistantMessage,
            boolean needsConfirmation,
            String confirmationReason) {
    }
}
