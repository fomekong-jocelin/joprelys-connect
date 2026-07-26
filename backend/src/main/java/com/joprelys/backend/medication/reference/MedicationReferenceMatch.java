package com.joprelys.backend.medication.reference;

public record MedicationReferenceMatch(
        String proposedName,
        String matchedName,
        String source,
        String conceptId,
        String evidence) {
}
