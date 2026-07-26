package com.joprelys.backend.ai.medication;

import java.util.List;

/**
 * Separate boundary for authoritative drug-drug interaction data.
 *
 * <p>RxNorm/RxClass is deliberately not treated as an interaction database.
 * An interaction provider can be introduced later without changing the
 * medication identity or clinical proposal pipeline.</p>
 */
public interface MedicationInteractionProvider {

    InteractionResult check(List<MedicationKnowledgeProvider.MedicationKnowledge> medications);

    record InteractionResult(
            Status status,
            List<MedicationInteraction> interactions,
            String source) {

        public InteractionResult {
            interactions = interactions == null ? List.of() : List.copyOf(interactions);
        }

        public static InteractionResult unavailable(String source) {
            return new InteractionResult(Status.UNAVAILABLE, List.of(), source);
        }
    }

    record MedicationInteraction(
            String leftConceptId,
            String rightConceptId,
            String severity,
            String description,
            String evidenceId) {
    }

    enum Status {
        CHECKED,
        UNAVAILABLE
    }
}
