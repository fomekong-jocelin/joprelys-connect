package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Narrow deterministic medication safety guard.
 *
 * <p>This intentionally checks only conflicts that can be established without
 * a pharmacological knowledge base: exact medication duplication and an exact
 * medication name explicitly present in documented allergies. Broader drug
 * classes and interactions require an authoritative medication referential and
 * must not be guessed here.</p>
 */
final class AiMedicationSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(AiMedicationSafetyGuard.class);
    private static final String PRESCRIPTION = "prescription";

    private final ObjectMapper objectMapper;

    AiMedicationSafetyGuard(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedResponse enforce(
            ParsedResponse parsed,
            Map<String, Object> clinicalContext,
            String locale) {
        if (parsed == null || parsed.needsClarification()) {
            return parsed;
        }
        ParsedChange prescriptionChange = parsed.changes().stream()
                .filter(change -> PRESCRIPTION.equals(change.field()) && "SET".equals(change.operation()))
                .findFirst()
                .orElse(null);
        if (prescriptionChange == null) {
            return parsed;
        }

        List<String> drugNames = proposedDrugNames(prescriptionChange.proposedValue());
        if (drugNames.isEmpty()) {
            return parsed;
        }

        for (String drugName : drugNames) {
            if (matchesDocumentedAllergy(drugName, clinicalContext)) {
                return requireConfirmation(parsed, drugName, SafetyReason.DOCUMENTED_ALLERGY, locale);
            }
            if (matchesActiveMedication(drugName, clinicalContext)) {
                return requireConfirmation(parsed, drugName, SafetyReason.ACTIVE_DUPLICATE, locale);
            }
        }
        return parsed;
    }

    private ParsedResponse requireConfirmation(
            ParsedResponse parsed,
            String drugName,
            SafetyReason reason,
            String locale) {
        String question = safetyQuestion(drugName, reason, locale);
        List<ParsedChange> safeChanges = parsed.changes().stream()
                .filter(change -> !PRESCRIPTION.equals(change.field()))
                .toList();
        log.warn("Medication proposal held for clinician confirmation reason={} drug={}", reason, drugName);
        return new ParsedResponse(
                safeChanges,
                question,
                true,
                new ParsedClarification(PRESCRIPTION, question, List.of()));
    }

    @SuppressWarnings("unchecked")
    private List<String> proposedDrugNames(String proposedValue) {
        if (proposedValue == null || proposedValue.isBlank()) {
            return List.of();
        }
        try {
            Object value = objectMapper.readValue(proposedValue, Object.class);
            if (!(value instanceof List<?> list)) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map
                        && map.get("drugName") instanceof String name
                        && !name.isBlank()) {
                    result.add(name.trim());
                }
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean matchesDocumentedAllergy(
            String drugName,
            Map<String, Object> clinicalContext) {
        Object patientValue = clinicalContext == null ? null : clinicalContext.get("patient");
        if (!(patientValue instanceof Map<?, ?> patient)) {
            return false;
        }
        Object allergyValue = patient.get("allergies");
        if (!(allergyValue instanceof String allergies) || allergies.isBlank()) {
            return false;
        }
        String normalizedDrug = normalize(drugName);
        String normalizedAllergies = normalize(allergies);
        return containsWholeTerm(normalizedAllergies, normalizedDrug);
    }

    @SuppressWarnings("unchecked")
    private boolean matchesActiveMedication(
            String drugName,
            Map<String, Object> clinicalContext) {
        Object medicationsValue = clinicalContext == null ? null : clinicalContext.get("activeMedications");
        if (!(medicationsValue instanceof List<?> medications)) {
            return false;
        }
        String normalizedDrug = normalize(drugName);
        for (Object value : medications) {
            if (value instanceof Map<?, ?> medication
                    && medication.get("drugName") instanceof String activeName
                    && normalize(activeName).equals(normalizedDrug)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsWholeTerm(String haystack, String needle) {
        if (needle.isBlank()) {
            return false;
        }
        return (" " + haystack + " ").contains(" " + needle + " ")
                || haystack.equals(needle);
    }

    private String safetyQuestion(String drugName, SafetyReason reason, String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return reason == SafetyReason.DOCUMENTED_ALLERGY
                    ? "The record lists an allergy to " + drugName + ". Do you confirm this prescription?"
                    : drugName + " is already listed as an active medication. Do you confirm adding it to this prescription?";
        }
        return reason == SafetyReason.DOCUMENTED_ALLERGY
                ? "Le dossier mentionne une allergie à " + drugName + ". Confirmez-vous cette prescription ?"
                : drugName + " figure déjà parmi les traitements actifs. Confirmez-vous son ajout à cette ordonnance ?";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private enum SafetyReason {
        DOCUMENTED_ALLERGY,
        ACTIVE_DUPLICATE
    }
}
