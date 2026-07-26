package com.joprelys.backend.medication.reference;

public record MedicationReferenceMatch(
        String proposedName,
        String matchedName,
        MedicationEquivalence.Status status,
        String source,
        String proposedConceptId,
        String matchedConceptId,
        String evidence) {
}
