package com.joprelys.backend.ai.facts.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ClinicalFactRevisionPlannerSchema {

    private ClinicalFactRevisionPlannerSchema() {
    }

    static Map<String, Object> schema() {
        Map<String, Object> evidence = evidenceSchema();
        Map<String, Object> fact = factSchema(evidence);
        Map<String, Object> retraction = retractionSchema(evidence);

        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("type", "object");
        operation.put("additionalProperties", false);
        operation.put("properties", Map.of(
                "type", enumString(List.of("KEEP", "ADD", "REPLACE", "RETRACT")),
                "targetFactId", nullableString(64),
                "fact", nullableObject(fact),
                "retraction", nullableObject(retraction)));
        operation.put("required", List.of("type", "targetFactId", "fact", "retraction"));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.put("properties", Map.of(
                "operations", Map.of(
                        "type", "array",
                        "maxItems", 64,
                        "items", operation)));
        root.put("required", List.of("operations"));
        return Map.copyOf(root);
    }

    private static Map<String, Object> evidenceSchema() {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("type", "object");
        evidence.put("additionalProperties", false);
        evidence.put("properties", Map.of(
                "transcriptItemId", string(64),
                "quoteText", string(2000),
                "primarySupport", Map.of("type", "boolean")));
        evidence.put("required", List.of("transcriptItemId", "quoteText", "primarySupport"));
        return evidence;
    }

    private static Map<String, Object> factSchema(Map<String, Object> evidence) {
        Map<String, Object> fact = new LinkedHashMap<>();
        fact.put("type", "object");
        fact.put("additionalProperties", false);
        fact.put("properties", Map.ofEntries(
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
                Map.entry("evidence", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "maxItems", 8,
                        "items", evidence))));
        fact.put("required", List.of(
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
                "evidence"));
        return fact;
    }

    private static Map<String, Object> retractionSchema(Map<String, Object> evidence) {
        Map<String, Object> retraction = new LinkedHashMap<>();
        retraction.put("type", "object");
        retraction.put("additionalProperties", false);
        retraction.put("properties", Map.of(
                "authority", enumString(List.of(
                        "PATIENT_REPORTED", "CLINICIAN_OBSERVED", "CLINICIAN_DECISION")),
                "reason", enumString(List.of(
                        "EXPLICIT_CORRECTION", "EXPLICIT_NEGATION", "CLINICIAN_CANCELLATION")),
                "evidence", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "maxItems", 8,
                        "items", evidence)));
        retraction.put("required", List.of("authority", "reason", "evidence"));
        return retraction;
    }

    private static Map<String, Object> nullableObject(Map<String, Object> schema) {
        return Map.of(
                "anyOf", List.of(
                        schema,
                        Map.of("type", "null")));
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
