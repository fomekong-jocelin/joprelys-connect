package com.joprelys.backend.ai.benchmark;

import java.util.List;

final class ClinicalBenchmarkModel {

    private ClinicalBenchmarkModel() {
    }

    enum Pipeline {
        FACT_EXTRACTION_196,
        ROLLING_PLANNER_211
    }

    record TranscriptTurn(
            String id,
            long startOffsetMs,
            long endOffsetMs,
            String speaker,
            String text) {
    }

    record Evidence(
            String transcriptTurnId,
            String quoteText) {
    }

    record Fact(
            String key,
            boolean critical,
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
            List<Evidence> evidence) {
    }

    record Operation(
            String type,
            String targetFactKey,
            Fact resultFact) {
    }

    record Scenario(
            String id,
            String locale,
            int durationMinutes,
            List<TranscriptTurn> transcript,
            List<Fact> initialFacts,
            List<Fact> expectedEffectiveFacts,
            List<Operation> expectedOperations) {
    }

    record CandidateRun(
            String scenarioId,
            Pipeline pipeline,
            String model,
            String promptVersion,
            List<Fact> effectiveFacts,
            List<Operation> operations) {
    }

    record Score(
            String scenarioId,
            Pipeline pipeline,
            int expectedFactCount,
            int candidateFactCount,
            int factTruePositives,
            int factFalsePositives,
            int factFalseNegatives,
            double factPrecision,
            double factRecall,
            int missingCriticalFacts,
            int unsupportedCriticalClaims,
            int criticalRelationErrors,
            int expectedEvidenceCount,
            int candidateEvidenceCount,
            int supportedEvidenceCount,
            int recoveredEvidenceCount,
            double evidencePrecision,
            double evidenceRecall,
            int expectedOperationCount,
            int candidateOperationCount,
            int operationTruePositives,
            int operationFalsePositives,
            int operationFalseNegatives,
            double operationPrecision,
            double operationRecall,
            int falseCriticalRetracts,
            int falseCriticalReplaces,
            boolean absoluteSafetyGatePassed) {
    }
}
