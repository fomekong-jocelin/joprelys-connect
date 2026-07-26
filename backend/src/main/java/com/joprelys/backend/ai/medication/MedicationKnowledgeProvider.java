package com.joprelys.backend.ai.medication;

import java.util.List;
import java.util.Optional;

/**
 * Provider-agnostic medication referential used by clinical safety guards.
 *
 * <p>The provider resolves a clinician-entered drug name into a canonical drug
 * identity, its clinically significant ingredients and classification metadata.
 * It does not make therapeutic decisions.</p>
 */
public interface MedicationKnowledgeProvider {

    Optional<MedicationKnowledge> resolve(String drugName);

    record MedicationKnowledge(
            String source,
            String conceptId,
            String canonicalName,
            List<MedicationIngredient> ingredients,
            List<MedicationClass> classes) {

        public MedicationKnowledge {
            ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
            classes = classes == null ? List.of() : List.copyOf(classes);
        }
    }

    record MedicationIngredient(
            String conceptId,
            String name,
            String termType) {
    }

    record MedicationClass(
            String source,
            String classId,
            String className,
            String classType,
            String relationship) {
    }
}
