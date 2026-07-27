package com.joprelys.backend.ai.benchmark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.CandidateRun;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Evidence;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Fact;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Operation;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Pipeline;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Scenario;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicalBenchmarkScorerTest {

    private final ClinicalBenchmarkScorer scorer = new ClinicalBenchmarkScorer();

    @Test
    void shouldPassAllAbsoluteSafetyGatesForGoldEquivalentRuns() {
        for (Scenario scenario : ClinicalBenchmarkCorpusV1.scenarios()) {
            var score = scorer.score(scenario, goldRun(scenario, Pipeline.ROLLING_PLANNER_211));

            assertThat(score.absoluteSafetyGatePassed()).as(scenario.id()).isTrue();
            assertThat(score.factPrecision()).isEqualTo(1.0d);
            assertThat(score.factRecall()).isEqualTo(1.0d);
            assertThat(score.evidencePrecision()).isEqualTo(1.0d);
            assertThat(score.evidenceRecall()).isEqualTo(1.0d);
            assertThat(score.operationPrecision()).isEqualTo(1.0d);
            assertThat(score.operationRecall()).isEqualTo(1.0d);
            assertThat(score.unsupportedCriticalClaims()).isZero();
            assertThat(score.criticalEvidenceErrors()).isZero();
        }
    }

    @Test
    void shouldCountMissingCriticalFactAndItsEvidenceInRecall() {
        Scenario scenario = scenario("fr-5m-baseline");
        List<Fact> facts = scenario.expectedEffectiveFacts().stream()
                .filter(fact -> !"BP".equals(fact.conceptCode()))
                .toList();

        var score = scorer.score(scenario, run(scenario, facts, scenario.expectedOperations()));

        assertThat(score.missingCriticalFacts()).isEqualTo(1);
        assertThat(score.factFalseNegatives()).isEqualTo(1);
        assertThat(score.expectedEvidenceCount()).isEqualTo(3);
        assertThat(score.candidateEvidenceCount()).isEqualTo(2);
        assertThat(score.evidenceRecall()).isLessThan(1.0d);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldFailOnUnsupportedCriticalClaim() {
        Scenario scenario = scenario("fr-5m-baseline");
        Fact hallucinatedHeartRate = new Fact(
                "candidate-heart-rate",
                true,
                "VITAL",
                "CLINICIAN_OBSERVED",
                "HEART_RATE",
                "fréquence cardiaque",
                "POSITIVE",
                "120",
                null,
                "BPM",
                null,
                "UNSPECIFIED",
                null,
                null,
                List.of(new Evidence("s5-bp", "120")));
        List<Fact> facts = new ArrayList<>(scenario.expectedEffectiveFacts());
        facts.add(hallucinatedHeartRate);

        var score = scorer.score(scenario, run(scenario, facts, scenario.expectedOperations()));

        assertThat(score.unsupportedCriticalClaims()).isEqualTo(1);
        assertThat(score.factPrecision()).isLessThan(1.0d);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldFailWhenCriticalNumberUnitRelationChanges() {
        Scenario scenario = scenario("fr-5m-baseline");
        Fact expectedBp = factByConcept(scenario, "BP");
        Fact wrongBp = copyFact(
                expectedBp,
                "candidate-wrong-bp",
                expectedBp.evidence(),
                "80",
                "120",
                "MMHG");
        List<Fact> facts = scenario.expectedEffectiveFacts().stream()
                .map(fact -> "BP".equals(fact.conceptCode()) ? wrongBp : fact)
                .toList();

        var score = scorer.score(scenario, run(scenario, facts, scenario.expectedOperations()));

        assertThat(score.criticalRelationErrors()).isEqualTo(1);
        assertThat(score.unsupportedCriticalClaims()).isEqualTo(1);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldFailWhenCriticalFactHasWrongLinkedEvidence() {
        Scenario scenario = scenario("fr-5m-baseline");
        Fact expectedBp = factByConcept(scenario, "BP");
        Fact wrongEvidenceBp = copyFact(
                expectedBp,
                "candidate-bp-wrong-evidence",
                List.of(new Evidence(
                        "s5-pain",
                        "douleur abdominale depuis trois jours à droite")),
                expectedBp.valuePrimary(),
                expectedBp.valueSecondary(),
                expectedBp.unitCode());
        List<Fact> facts = scenario.expectedEffectiveFacts().stream()
                .map(fact -> "BP".equals(fact.conceptCode()) ? wrongEvidenceBp : fact)
                .toList();

        var score = scorer.score(scenario, run(scenario, facts, scenario.expectedOperations()));

        assertThat(score.factPrecision()).isEqualTo(1.0d);
        assertThat(score.factRecall()).isEqualTo(1.0d);
        assertThat(score.criticalEvidenceErrors()).isEqualTo(1);
        assertThat(score.evidencePrecision()).isLessThan(1.0d);
        assertThat(score.evidenceRecall()).isLessThan(1.0d);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldFailOnFalseCriticalRetractionEvenWhenFinalFactsLookCorrect() {
        Scenario scenario = scenario("fr-60m-longitudinal-corrections");
        List<Operation> operations = new ArrayList<>();
        for (Operation operation : scenario.expectedOperations()) {
            if ("KEEP".equals(operation.type())) {
                operations.add(new Operation(
                        "RETRACT",
                        operation.targetFactKey(),
                        null,
                        "EXPLICIT_NEGATION",
                        List.of(new Evidence(
                                "s60-diabetes-restated",
                                "toujours mon diabète de type 2"))));
            } else {
                operations.add(operation);
            }
        }

        var score = scorer.score(scenario, run(scenario, scenario.expectedEffectiveFacts(), operations));

        assertThat(score.factPrecision()).isEqualTo(1.0d);
        assertThat(score.factRecall()).isEqualTo(1.0d);
        assertThat(score.falseCriticalRetracts()).isEqualTo(1);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldFailOnFalseCriticalReplaceEvenWhenFinalFactsLookCorrect() {
        Scenario scenario = scenario("fr-60m-longitudinal-corrections");
        List<Operation> operations = new ArrayList<>();
        for (Operation operation : scenario.expectedOperations()) {
            if ("REPLACE".equals(operation.type())
                    && operation.resultFact() != null
                    && "PARACETAMOL".equals(operation.resultFact().conceptCode())) {
                Fact wrongDose = copyFact(
                        operation.resultFact(),
                        "candidate-paracetamol-650",
                        operation.resultFact().evidence(),
                        "650",
                        null,
                        "MG");
                operations.add(new Operation(
                        "REPLACE",
                        operation.targetFactKey(),
                        wrongDose,
                        null,
                        List.of()));
            } else {
                operations.add(operation);
            }
        }

        var score = scorer.score(scenario, run(scenario, scenario.expectedEffectiveFacts(), operations));

        assertThat(score.falseCriticalReplaces()).isEqualTo(1);
        assertThat(score.operationPrecision()).isLessThan(1.0d);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldScoreRetractionReasonAndEvidenceNotOnlyTarget() {
        Scenario scenario = scenario("fr-30m-explicit-retractions");
        List<Operation> operations = new ArrayList<>(scenario.expectedOperations());
        Operation medicationRetraction = operations.getFirst();
        operations.set(0, new Operation(
                medicationRetraction.type(),
                medicationRetraction.targetFactKey(),
                null,
                "CLINICIAN_CANCELLATION",
                List.of(new Evidence("s30-order-cancel", "annule le scanner abdominal"))));

        var score = scorer.score(scenario, run(scenario, scenario.expectedEffectiveFacts(), operations));

        assertThat(score.falseCriticalRetracts()).isEqualTo(1);
        assertThat(score.operationFalsePositives()).isEqualTo(1);
        assertThat(score.operationFalseNegatives()).isEqualTo(1);
        assertThat(score.absoluteSafetyGatePassed()).isFalse();
    }

    @Test
    void shouldRejectDuplicateOperationsInsteadOfHidingThemInASet() {
        Scenario scenario = scenario("fr-5m-baseline");
        List<Operation> operations = new ArrayList<>(scenario.expectedOperations());
        operations.add(scenario.expectedOperations().getFirst());

        assertThatThrownBy(() -> scorer.score(
                scenario,
                run(scenario, scenario.expectedEffectiveFacts(), operations)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate candidate operation");
    }

    private Scenario scenario(String id) {
        return ClinicalBenchmarkCorpusV1.scenarios().stream()
                .filter(candidate -> id.equals(candidate.id()))
                .findFirst()
                .orElseThrow();
    }

    private Fact factByConcept(Scenario scenario, String conceptCode) {
        return scenario.expectedEffectiveFacts().stream()
                .filter(fact -> conceptCode.equals(fact.conceptCode()))
                .findFirst()
                .orElseThrow();
    }

    private CandidateRun goldRun(Scenario scenario, Pipeline pipeline) {
        return new CandidateRun(
                scenario.id(),
                pipeline,
                "gold-fixture",
                ClinicalBenchmarkCorpusV1.VERSION,
                scenario.expectedEffectiveFacts(),
                scenario.expectedOperations());
    }

    private CandidateRun run(
            Scenario scenario,
            List<Fact> facts,
            List<Operation> operations) {
        return new CandidateRun(
                scenario.id(),
                Pipeline.ROLLING_PLANNER_211,
                "candidate-fixture",
                ClinicalBenchmarkCorpusV1.VERSION,
                List.copyOf(facts),
                List.copyOf(operations));
    }

    private Fact copyFact(
            Fact source,
            String key,
            List<Evidence> evidence,
            String valuePrimary,
            String valueSecondary,
            String unitCode) {
        return new Fact(
                key,
                source.critical(),
                source.factType(),
                source.authority(),
                source.conceptCode(),
                source.conceptText(),
                source.polarity(),
                valuePrimary,
                valueSecondary,
                unitCode,
                source.temporalityText(),
                source.laterality(),
                source.frequencyText(),
                source.routeText(),
                List.copyOf(evidence));
    }
}
