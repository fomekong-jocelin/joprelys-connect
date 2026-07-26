package com.joprelys.backend.medication.reference;

import java.util.List;
import java.util.Set;

public interface MedicationReferencePort {

    String source();

    default int priority() {
        return 100;
    }

    List<MedicationConcept> findCandidates(String query, int maximum);

    /**
     * Returns authoritative ingredient concept identifiers for a resolved concept.
     * Providers that cannot establish ingredient identity must return an empty set.
     */
    default Set<String> findIngredientConceptIds(MedicationConcept concept) {
        if (concept == null || concept.conceptId() == null || concept.conceptId().isBlank()) {
            return Set.of();
        }
        String termType = concept.termType();
        if ("IN".equals(termType) || "PIN".equals(termType) || "MIN".equals(termType)) {
            return Set.of(concept.conceptId());
        }
        return Set.of();
    }
}
