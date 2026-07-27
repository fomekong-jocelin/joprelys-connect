package com.joprelys.backend.ai.facts.application;

import java.util.List;

final class ClinicalFactRevisionPlannerModelContract {

    private ClinicalFactRevisionPlannerModelContract() {
    }

    record PlannerEnvelope(List<PlannerOperation> operations) {
    }

    record PlannerOperation(
            String type,
            String targetFactId,
            PlannerFact fact,
            PlannerRetraction retraction) {
    }

    record PlannerFact(
            String factType,
            String authority,
            String conceptCode,
            String conceptText,
            String polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporalityText,
            String laterality,
            String frequencyText,
            String routeText,
            List<PlannerEvidence> evidence) {
    }

    record PlannerRetraction(
            String authority,
            String reason,
            List<PlannerEvidence> evidence) {
    }

    record PlannerEvidence(
            String transcriptItemId,
            String quoteText,
            boolean primarySupport) {
    }
}
