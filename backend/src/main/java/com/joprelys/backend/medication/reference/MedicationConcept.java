package com.joprelys.backend.medication.reference;

import java.util.List;

public record MedicationConcept(
        String source,
        String conceptId,
        String canonicalName,
        String termType,
        List<String> synonyms,
        boolean active) {

    public MedicationConcept {
        synonyms = synonyms == null ? List.of() : List.copyOf(synonyms);
    }
}
