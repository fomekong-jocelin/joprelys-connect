package com.joprelys.backend.ai.facts.application;

import java.util.List;

final class ClinicalFactRevisionPlannerModelContract {

    private ClinicalFactRevisionPlannerModelContract() {
    }

    public record PlannerEnvelope(List<PlannerOperation> operations) {
    }

    public record PlannerOperation(
            String type,
            String targetFactId,
            PlannerFact fact,
            PlannerRetraction retraction) {
    }

    public record PlannerFact(
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

    public record PlannerRetraction(
            String authority,
            String reason,
            List<PlannerEvidence> evidence) {
    }

    public record PlannerEvidence(
            String transcriptItemId,
            String quoteText,
            boolean primarySupport) {
    }
}
