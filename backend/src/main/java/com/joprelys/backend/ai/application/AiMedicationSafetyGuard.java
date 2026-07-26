package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.medication.reference.MedicationReferenceDuplicateDetector;
import com.joprelys.backend.medication.reference.MedicationReferenceMatch;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Deterministic medication safety boundary applied before a prescription can
 * become a clinician-facing revision.
 *
 * <p>Exact local checks always run first. When a medication reference is
 * available, Joprelys can additionally hold a prescription when two different
 * names are proven to represent the same reference concept or active
 * ingredient. Absence of reference evidence is never interpreted as safe.</p>
 */
final class AiMedicationSafetyGuard {

    private static final Logger log = LoggerFactory.getLogger(AiMedicationSafetyGuard.class);
    private static final String PRESCRIPTION = "prescription";

    private final ObjectMapper objectMapper;
    private MedicationReferenceDuplicateDetector referenceDuplicateDetector;

    AiMedicationSafetyGuard(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    AiMedicationSafetyGuard(
            ObjectMapper objectMapper,
            MedicationReferenceDuplicateDetector referenceDuplicateDetector) {
        this.objectMapper = objectMapper;
        this.referenceDuplicateDetector = referenceDuplicateDetector;
    }

    void setReferenceDuplicateDetector(
            MedicationReferenceDuplicateDetector referenceDuplicateDetector) {
        this.referenceDuplicateDetector = referenceDuplicateDetector;
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
                return requireConfirmation(
                        parsed, drugName, null, SafetyReason.DOCUMENTED_ALLERGY, locale);
            }
            if (matchesActiveMedication(drugName, clinicalContext)) {
                return requireConfirmation(
                        parsed, drugName, null, SafetyReason.ACTIVE_DUPLICATE, locale);
            }

            Optional<MedicationReferenceMatch> allergyMatch = findReferenceAllergy(
                    drugName, clinicalContext);
            if (allergyMatch.isPresent()) {
                return requireConfirmation(
                        parsed,
                        drugName,
                        allergyMatch.orElseThrow(),
                        SafetyReason.REFERENCE_DOCUMENTED_ALLERGY,
                        locale);
            }

            Optional<MedicationReferenceMatch> activeMatch = findReferenceActiveMedication(
                    drugName, clinicalContext);
            if (activeMatch.isPresent()) {
                return requireConfirmation(
                        parsed,
                        drugName,
                        activeMatch.orElseThrow(),
                        SafetyReason.REFERENCE_ACTIVE_DUPLICATE,
                        locale);
            }
        }
        return parsed;
    }

    private ParsedResponse requireConfirmation(
            ParsedResponse parsed,
            String drugName,
            MedicationReferenceMatch referenceMatch,
            SafetyReason reason,
            String locale) {
        String question = safetyQuestion(drugName, referenceMatch, reason, locale);
        List<ParsedChange> safeChanges = parsed.changes().stream()
                .filter(change -> !PRESCRIPTION.equals(change.field()))
                .toList();
        if (referenceMatch == null) {
            log.warn(
                    "Medication proposal held for clinician confirmation reason={} drug={}",
                    reason,
                    drugName);
        } else {
            log.warn(
                    "Medication proposal held for reference confirmation reason={} drug={} matched={} source={} status={} proposedConcept={} matchedConcept={}",
                    reason,
                    drugName,
                    referenceMatch.matchedName(),
                    referenceMatch.source(),
                    referenceMatch.status(),
                    referenceMatch.proposedConceptId(),
                    referenceMatch.matchedConceptId());
        }
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

    private boolean matchesActiveMedication(
            String drugName,
            Map<String, Object> clinicalContext) {
        String normalizedDrug = normalize(drugName);
        return activeMedicationNames(clinicalContext).stream()
                .map(this::normalize)
                .anyMatch(normalizedDrug::equals);
    }

    private Optional<MedicationReferenceMatch> findReferenceAllergy(
            String drugName,
            Map<String, Object> clinicalContext) {
        if (referenceDuplicateDetector == null) {
            return Optional.empty();
        }
        return referenceDuplicateDetector.findEquivalent(
                drugName,
                documentedAllergyEntries(clinicalContext));
    }

    private Optional<MedicationReferenceMatch> findReferenceActiveMedication(
            String drugName,
            Map<String, Object> clinicalContext) {
        if (referenceDuplicateDetector == null) {
            return Optional.empty();
        }
        return referenceDuplicateDetector.findEquivalent(
                drugName,
                activeMedicationNames(clinicalContext));
    }

    private List<String> documentedAllergyEntries(Map<String, Object> clinicalContext) {
        Object patientValue = clinicalContext == null ? null : clinicalContext.get("patient");
        if (!(patientValue instanceof Map<?, ?> patient)) {
            return List.of();
        }
        Object allergyValue = patient.get("allergies");
        if (!(allergyValue instanceof String allergies) || allergies.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allergies.split("[;,|\\n\\r]+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private List<String> activeMedicationNames(Map<String, Object> clinicalContext) {
        Object medicationsValue = clinicalContext == null ? null : clinicalContext.get("activeMedications");
        if (!(medicationsValue instanceof List<?> medications)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (Object value : medications) {
            if (value instanceof Map<?, ?> medication
                    && medication.get("drugName") instanceof String activeName
                    && !activeName.isBlank()) {
                names.add(activeName.trim());
            }
        }
        return List.copyOf(names);
    }

    private boolean containsWholeTerm(String haystack, String needle) {
        if (needle.isBlank()) {
            return false;
        }
        return (" " + haystack + " ").contains(" " + needle + " ")
                || haystack.equals(needle);
    }

    private String safetyQuestion(
            String drugName,
            MedicationReferenceMatch referenceMatch,
            SafetyReason reason,
            String locale) {
        if ("en".equalsIgnoreCase(locale)) {
            return switch (reason) {
                case DOCUMENTED_ALLERGY ->
                        "The record lists an allergy to " + drugName + ". Do you confirm this prescription?";
                case ACTIVE_DUPLICATE ->
                        drugName + " is already listed as an active medication. Do you confirm adding it to this prescription?";
                case REFERENCE_DOCUMENTED_ALLERGY ->
                        "Joprelys matches " + drugName + " to the documented allergy "
                                + referenceMatch.matchedName() + " in the medication reference. Do you confirm this prescription?";
                case REFERENCE_ACTIVE_DUPLICATE ->
                        "Joprelys matches " + drugName + " to the active medication "
                                + referenceMatch.matchedName() + " in the medication reference. Do you confirm adding it?";
            };
        }
        return switch (reason) {
            case DOCUMENTED_ALLERGY ->
                    "Le dossier mentionne une allergie à " + drugName + ". Confirmez-vous cette prescription ?";
            case ACTIVE_DUPLICATE ->
                    drugName + " figure déjà parmi les traitements actifs. Confirmez-vous son ajout à cette ordonnance ?";
            case REFERENCE_DOCUMENTED_ALLERGY ->
                    "Joprelys rapproche " + drugName + " de l'allergie documentée "
                            + referenceMatch.matchedName() + " dans le référentiel médicament. Confirmez-vous cette prescription ?";
            case REFERENCE_ACTIVE_DUPLICATE ->
                    "Joprelys rapproche " + drugName + " du traitement actif "
                            + referenceMatch.matchedName() + " dans le référentiel médicament. Confirmez-vous son ajout ?";
        };
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
        ACTIVE_DUPLICATE,
        REFERENCE_DOCUMENTED_ALLERGY,
        REFERENCE_ACTIVE_DUPLICATE
    }
}
