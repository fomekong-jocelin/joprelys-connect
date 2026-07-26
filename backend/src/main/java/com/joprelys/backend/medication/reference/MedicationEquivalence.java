package com.joprelys.backend.medication.reference;

public record MedicationEquivalence(
        Status status,
        String source,
        String leftConceptId,
        String rightConceptId,
        String explanation) {

    public enum Status {
        SAME_CONCEPT,
        SAME_INGREDIENT,
        UNKNOWN
    }

    public static MedicationEquivalence unknown(String explanation) {
        return new MedicationEquivalence(
                Status.UNKNOWN,
                null,
                null,
                null,
                explanation);
    }
}
