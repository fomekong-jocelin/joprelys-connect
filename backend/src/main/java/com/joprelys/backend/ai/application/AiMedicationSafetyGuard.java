package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.ai.medication.MedicationSafetyEngine;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

final class AiMedicationSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(AiMedicationSafetyGuard.class);
    private static final String PRESCRIPTION = "prescription";

    private final ObjectMapper objectMapper;
    private final MedicationSafetyEngine safetyEngine;

    AiMedicationSafetyGuard(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    AiMedicationSafetyGuard(
            ObjectMapper objectMapper,
            MedicationSafetyEngine safetyEngine) {
        this.objectMapper = objectMapper;
        this.safetyEngine = safetyEngine;
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
            if (safetyEngine != null) {
                MedicationSafetyEngine.Assessment assessment = safetyEngine.assess(drugName, clinicalContext);
                if (assessment.requiresConfirmation()) {
                    return requireConfirmation(parsed, assessment, locale);
                }
                continue;
            }
            if (matchesDocumentedAllergy(drugName, clinicalContext)) {
                return requireLegacyConfirmation(parsed, drugName, LegacySafetyReason.DOCUMENTED_ALLERGY, locale);
            }
            if (matchesActiveMedication(drugName, clinicalContext)) {
                return requireLegacyConfirmation(parsed, drugName, LegacySafetyReason.ACTIVE_DUPLICATE, locale);
            }
        }
        return parsed;
    }

    private ParsedResponse requireConfirmation(
            ParsedResponse parsed,
            MedicationSafetyEngine.Assessment assessment,
            String locale) {
        String question = safetyQuestion(assessment, locale);
        log.warn(
                "Medication proposal held for clinician confirmation reason={} drug={} source={}",
                assessment.findingType(),
                assessment.medication(),
                assessment.source());
        return holdPrescription(parsed, question);
    }

    private ParsedResponse requireLegacyConfirmation(
            ParsedResponse parsed,
            String drugName,
            LegacySafetyReason reason,
            String locale) {
        String question = legacySafetyQuestion(drugName, reason, locale);
        log.warn("Medication proposal held for clinician confirmation reason={} drug={}", reason, drugName);
        return holdPrescription(parsed, question);
    }

    private ParsedResponse holdPrescription(ParsedResponse parsed, String question) {
        List<ParsedChange> safeChanges = parsed.changes().stream()
                .filter(change -> !PRESCRIPTION.equals(change.field()))
                .toList();
        return new ParsedResponse(
                safeChanges,
                question,
                true,
                new ParsedClarification(PRESCRIPTION, question, List.of()));
    }

    private String safetyQuestion(
            MedicationSafetyEngine.Assessment assessment,
            String locale) {
        boolean english = "en".equalsIgnoreCase(locale);
        String medication = assessment.medication();
        String related = assessment.relatedMedicationOrFact();
        return switch (assessment.findingType()) {
            case DOCUMENTED_ALLERGY_EXACT -> english
                    ? "The record lists an allergy to " + medication + ". Do you confirm this prescription?"
                    : "Le dossier mentionne une allergie à " + medication + ". Confirmez-vous cette prescription ?";
            case DOCUMENTED_ALLERGY_INGREDIENT -> english
                    ? medication + " shares an RxNorm ingredient with the documented allergy “" + related
                            + "”. Do you confirm this prescription?"
                    : medication + " partage un ingrédient RxNorm avec l’allergie documentée « " + related
                            + " ». Confirmez-vous cette prescription ?";
            case ACTIVE_DUPLICATE_EXACT -> english
                    ? medication + " is already listed as an active medication. Do you confirm adding it to this prescription?"
                    : medication + " figure déjà parmi les traitements actifs. Confirmez-vous son ajout à cette ordonnance ?";
            case ACTIVE_DUPLICATE_INGREDIENT -> english
                    ? medication + " shares an active ingredient with “" + related
                            + "”, already listed as active treatment. Do you confirm?"
                    : medication + " partage un principe actif avec « " + related
                            + " », déjà présent dans les traitements actifs. Confirmez-vous ?";
            case DRUG_INTERACTION -> english
                    ? "An authoritative interaction source reports a potential interaction for " + medication
                            + ": " + related + ". Do you confirm after clinical review?"
                    : "Une source d’interactions autoritative signale une interaction potentielle pour " + medication
                            + " : " + related + ". Confirmez-vous après vérification clinique ?";
            case MEDICATION_UNRESOLVED -> english
                    ? "The medication “" + medication
                            + "” could not be identified in the medication referential. Please confirm the exact name before prescribing."
                    : "Le médicament « " + medication
                            + " » n’a pas pu être identifié dans le référentiel. Confirmez le nom exact avant de prescrire.";
            case REFERENTIAL_UNAVAILABLE -> english
                    ? "The medication safety referential is temporarily unavailable. Please verify “" + medication
                            + "” manually before confirming the prescription."
                    : "Le référentiel de sécurité médicament est temporairement indisponible. Vérifiez manuellement « "
                            + medication + " » avant de confirmer l’ordonnance.";
            case NONE -> english ? "Please confirm this prescription." : "Confirmez-vous cette prescription ?";
        };
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
        return (" " + haystack + " ").contains(" " + needle + " ") || haystack.equals(needle);
    }

    private String legacySafetyQuestion(
            String drugName,
            LegacySafetyReason reason,
            String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return reason == LegacySafetyReason.DOCUMENTED_ALLERGY
                    ? "The record lists an allergy to " + drugName + ". Do you confirm this prescription?"
                    : drugName + " is already listed as an active medication. Do you confirm adding it to this prescription?";
        }
        return reason == LegacySafetyReason.DOCUMENTED_ALLERGY
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

    private enum LegacySafetyReason {
        DOCUMENTED_ALLERGY,
        ACTIVE_DUPLICATE
    }
}
