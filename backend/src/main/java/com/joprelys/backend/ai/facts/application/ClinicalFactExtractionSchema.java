package com.joprelys.backend.ai.facts.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ClinicalFactExtractionSchema {

    private ClinicalFactExtractionSchema() {
    }

    static Map<String, Object> schema() {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("type", "object");
        fact.put("additionalProperties", false);
        fact.put("properties", Map.ofEntries(
                Map.entry("transcriptItemId", string(64)),
                Map.entry("factType", enumString(List.of(
                        "SYMPTOM", "VITAL", "MEDICATION", "ALLERGY", "HISTORY",
                        "ASSESSMENT", "PLAN", "ORDER"))),
                Map.entry("authority", enumString(List.of(
                        "PATIENT_REPORTED", "CLINICIAN_OBSERVED", "CLINICIAN_DECISION"))),
                Map.entry("conceptCode", string(64)),
                Map.entry("conceptText", string(256)),
                Map.entry("polarity", enumString(List.of("POSITIVE", "NEGATIVE", "UNCERTAIN"))),
                Map.entry("valuePrimary", nullableString(128)),
                Map.entry("valueSecondary", nullableString(128)),
                Map.entry("unitCode", nullableString(32)),
                Map.entry("temporalityText", nullableString(256)),
                Map.entry("laterality", enumString(List.of("LEFT", "RIGHT", "BILATERAL", "UNSPECIFIED"))),
                Map.entry("frequencyText", nullableString(128)),
                Map.entry("routeText", nullableString(64)),
                Map.entry("quoteText", string(2000))));
        fact.put("required", List.of(
                "transcriptItemId",
                "factType",
                "authority",
                "conceptCode",
                "conceptText",
                "polarity",
                "valuePrimary",
                "valueSecondary",
                "unitCode",
                "temporalityText",
                "laterality",
                "frequencyText",
                "routeText",
                "quoteText"));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.put("properties", Map.of(
                "facts", Map.of(
                        "type", "array",
                        "maxItems", 64,
                        "items", fact)));
        root.put("required", List.of("facts"));
        return Map.copyOf(root);
    }

    private static Map<String, Object> string(int maxLength) {
        return Map.of(
                "type", "string",
                "minLength", 1,
                "maxLength", maxLength);
    }

    private static Map<String, Object> nullableString(int maxLength) {
        return Map.of(
                "type", List.of("string", "null"),
                "maxLength", maxLength);
    }

    private static Map<String, Object> enumString(List<String> values) {
        return Map.of(
                "type", "string",
                "enum", values);
    }
}
