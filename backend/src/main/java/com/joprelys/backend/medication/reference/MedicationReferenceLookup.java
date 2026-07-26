package com.joprelys.backend.medication.reference;

import java.time.Instant;
import java.util.List;

public record MedicationReferenceLookup(
        String query,
        boolean resolved,
        boolean degraded,
        String source,
        List<MedicationConcept> candidates,
        String warning,
        Instant resolvedAt) {

    public MedicationReferenceLookup {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        resolvedAt = resolvedAt == null ? Instant.now() : resolvedAt;
    }

    public static MedicationReferenceLookup unresolved(
            String query,
            boolean degraded,
            String warning) {
        return new MedicationReferenceLookup(
                query,
                false,
                degraded,
                null,
                List.of(),
                warning,
                Instant.now());
    }
}
