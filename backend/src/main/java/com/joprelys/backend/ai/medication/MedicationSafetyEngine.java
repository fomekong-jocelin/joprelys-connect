package com.joprelys.backend.ai.medication;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MedicationSafetyEngine {

    private final MedicationKnowledgeProvider knowledgeProvider;
    private final MedicationInteractionProvider interactionProvider;
    private final MedicationSafetyProperties properties;

    public MedicationSafetyEngine(
            MedicationKnowledgeProvider knowledgeProvider,
            MedicationInteractionProvider interactionProvider,
            MedicationSafetyProperties properties) {
        this.knowledgeProvider = knowledgeProvider;
        this.interactionProvider = interactionProvider;
        this.properties = properties;
    }

    public Assessment assess(
            String proposedDrugName,
            Map<String, Object> clinicalContext) {
        String drugName = proposedDrugName == null ? "" : proposedDrugName.trim();
        if (drugName.isBlank() || !properties.enabled()) {
            return Assessment.safe();
        }

        String allergies = allergyText(clinicalContext);
        List<String> activeMedicationNames = activeMedicationNames(clinicalContext);

        if (containsWholeTerm(normalize(allergies), normalize(drugName))) {
            return Assessment.hold(FindingType.DOCUMENTED_ALLERGY_EXACT, drugName, drugName, "LOCAL");
        }
        if (activeMedicationNames.stream().anyMatch(active -> normalize(active).equals(normalize(drugName)))) {
            return Assessment.hold(FindingType.ACTIVE_DUPLICATE_EXACT, drugName, drugName, "LOCAL");
        }

        MedicationKnowledgeProvider.MedicationKnowledge proposed;
        try {
            Optional<MedicationKnowledgeProvider.MedicationKnowledge> resolved = knowledgeProvider.resolve(drugName);
            if (resolved.isEmpty()) {
                return properties.failClosed()
                        ? Assessment.hold(FindingType.MEDICATION_UNRESOLVED, drugName, null, "RXNORM")
                        : Assessment.safe();
            }
            proposed = resolved.get();
        } catch (MedicationKnowledgeUnavailableException exception) {
            return properties.failClosed()
                    ? Assessment.hold(FindingType.REFERENTIAL_UNAVAILABLE, drugName, null, "RXNORM")
                    : Assessment.safe();
        }

        Assessment allergyAssessment = assessAllergyIngredients(proposed, allergies);
        if (allergyAssessment.requiresConfirmation()) {
            return allergyAssessment;
        }

        List<MedicationKnowledgeProvider.MedicationKnowledge> resolvedActive = new ArrayList<>();
        for (String activeName : activeMedicationNames) {
            try {
                knowledgeProvider.resolve(activeName).ifPresent(active -> {
                    resolvedActive.add(active);
                });
            } catch (MedicationKnowledgeUnavailableException exception) {
                if (properties.failClosed()) {
                    return Assessment.hold(
                            FindingType.REFERENTIAL_UNAVAILABLE,
                            drugName,
                            activeName,
                            "RXNORM");
                }
            }
        }

        for (MedicationKnowledgeProvider.MedicationKnowledge active : resolvedActive) {
            if (sameConcept(proposed, active) || sharesIngredient(proposed, active)) {
                return Assessment.hold(
                        FindingType.ACTIVE_DUPLICATE_INGREDIENT,
                        drugName,
                        active.canonicalName(),
                        proposed.source());
            }
        }

        List<MedicationKnowledgeProvider.MedicationKnowledge> interactionSet = new ArrayList<>();
        interactionSet.add(proposed);
        interactionSet.addAll(resolvedActive);
        MedicationInteractionProvider.InteractionResult interactions = interactionProvider.check(interactionSet);
        if (interactions.status() == MedicationInteractionProvider.Status.CHECKED
                && !interactions.interactions().isEmpty()) {
            MedicationInteractionProvider.MedicationInteraction first = interactions.interactions().getFirst();
            return Assessment.hold(
                    FindingType.DRUG_INTERACTION,
                    drugName,
                    first.description(),
                    interactions.source());
        }

        return new Assessment(
                false,
                FindingType.NONE,
                drugName,
                proposed.canonicalName(),
                proposed.source(),
                proposed.classes());
    }

    private Assessment assessAllergyIngredients(
            MedicationKnowledgeProvider.MedicationKnowledge proposed,
            String allergies) {
        for (String allergyTerm : splitAllergyTerms(allergies)) {
            if (allergyTerm.isBlank()) {
                continue;
            }
            try {
                Optional<MedicationKnowledgeProvider.MedicationKnowledge> resolvedAllergy =
                        knowledgeProvider.resolve(allergyTerm);
                if (resolvedAllergy.isPresent()
                        && (sameConcept(proposed, resolvedAllergy.get())
                        || sharesIngredient(proposed, resolvedAllergy.get()))) {
                    return Assessment.hold(
                            FindingType.DOCUMENTED_ALLERGY_INGREDIENT,
                            proposed.canonicalName(),
                            allergyTerm,
                            proposed.source());
                }
            } catch (MedicationKnowledgeUnavailableException exception) {
                if (properties.failClosed()) {
                    return Assessment.hold(
                            FindingType.REFERENTIAL_UNAVAILABLE,
                            proposed.canonicalName(),
                            allergyTerm,
                            "RXNORM");
                }
            }
        }
        return Assessment.safe();
    }

    private boolean sameConcept(
            MedicationKnowledgeProvider.MedicationKnowledge left,
            MedicationKnowledgeProvider.MedicationKnowledge right) {
        return left.conceptId() != null && left.conceptId().equals(right.conceptId());
    }

    private boolean sharesIngredient(
            MedicationKnowledgeProvider.MedicationKnowledge left,
            MedicationKnowledgeProvider.MedicationKnowledge right) {
        Set<String> leftIngredients = ingredientIds(left);
        if (leftIngredients.isEmpty()) {
            return false;
        }
        Set<String> rightIngredients = ingredientIds(right);
        return leftIngredients.stream().anyMatch(rightIngredients::contains);
    }

    private Set<String> ingredientIds(MedicationKnowledgeProvider.MedicationKnowledge medication) {
        Set<String> ids = new HashSet<>();
        for (MedicationKnowledgeProvider.MedicationIngredient ingredient : medication.ingredients()) {
            if (ingredient.conceptId() != null && !ingredient.conceptId().isBlank()) {
                ids.add(ingredient.conceptId());
            }
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private String allergyText(Map<String, Object> clinicalContext) {
        Object patientValue = clinicalContext == null ? null : clinicalContext.get("patient");
        if (!(patientValue instanceof Map<?, ?> patient)) {
            return "";
        }
        Object value = patient.get("allergies");
        return value instanceof String text ? text : "";
    }

    @SuppressWarnings("unchecked")
    private List<String> activeMedicationNames(Map<String, Object> clinicalContext) {
        Object value = clinicalContext == null ? null : clinicalContext.get("activeMedications");
        if (!(value instanceof List<?> medications)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : medications) {
            if (item instanceof Map<?, ?> medication
                    && medication.get("drugName") instanceof String name
                    && !name.isBlank()) {
                result.add(name.trim());
            }
        }
        return List.copyOf(result);
    }

    private List<String> splitAllergyTerms(String allergies) {
        if (allergies == null || allergies.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(allergies.split("[;,/\\n]+"))
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .toList();
    }

    private boolean containsWholeTerm(String haystack, String needle) {
        if (needle.isBlank()) {
            return false;
        }
        return (" " + haystack + " ").contains(" " + needle + " ") || haystack.equals(needle);
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

    public enum FindingType {
        NONE,
        DOCUMENTED_ALLERGY_EXACT,
        DOCUMENTED_ALLERGY_INGREDIENT,
        ACTIVE_DUPLICATE_EXACT,
        ACTIVE_DUPLICATE_INGREDIENT,
        DRUG_INTERACTION,
        MEDICATION_UNRESOLVED,
        REFERENTIAL_UNAVAILABLE
    }

    public record Assessment(
            boolean requiresConfirmation,
            FindingType findingType,
            String medication,
            String relatedMedicationOrFact,
            String source,
            List<MedicationKnowledgeProvider.MedicationClass> classes) {

        public Assessment {
            classes = classes == null ? List.of() : List.copyOf(classes);
        }

        public static Assessment safe() {
            return new Assessment(false, FindingType.NONE, null, null, null, List.of());
        }

        public static Assessment hold(
                FindingType type,
                String medication,
                String relatedMedicationOrFact,
                String source) {
            return new Assessment(true, type, medication, relatedMedicationOrFact, source, List.of());
        }
    }
}
