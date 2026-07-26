package com.joprelys.backend.ai.application;

import java.util.Set;

/**
 * Single deterministic source of truth for physiological input ranges accepted
 * by Joprelys AI proposal paths. These are application safety bounds, not
 * diagnostic reference ranges.
 */
final class AiVitalsSafetyRules {

    static final Set<String> VITAL_FIELDS = Set.of(
            "temperature", "weight", "height", "pulse", "systolic", "diastolic",
            "spo2", "glycemia", "respiratoryRate", "painScale");

    private AiVitalsSafetyRules() {
    }

    static boolean isSafeProposal(String field, Number number) {
        if (field == null || !VITAL_FIELDS.contains(field) || number == null) {
            return false;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            return false;
        }
        return switch (field) {
            case "temperature" -> value >= 30 && value <= 45;
            case "weight" -> value >= 1 && value <= 500;
            case "height" -> value >= 30 && value <= 250;
            case "pulse" -> value >= 20 && value <= 250;
            case "systolic" -> value >= 40 && value <= 250;
            case "diastolic" -> value >= 30 && value <= 150;
            case "spo2" -> value >= 50 && value <= 100;
            case "glycemia" -> value >= 0.1 && value <= 10;
            case "respiratoryRate" -> value >= 5 && value <= 100;
            case "painScale" -> value >= 0 && value <= 10;
            default -> false;
        };
    }
}
