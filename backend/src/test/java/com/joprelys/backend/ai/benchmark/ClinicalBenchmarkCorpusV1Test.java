package com.joprelys.backend.ai.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Operation;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Scenario;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClinicalBenchmarkCorpusV1Test {

    @Test
    void shouldCoverRequiredFiveFifteenThirtyAndSixtyMinuteScenarios() {
        List<Scenario> scenarios = ClinicalBenchmarkCorpusV1.scenarios();

        assertThat(scenarios).hasSize(4);
        assertThat(scenarios.stream().map(Scenario::durationMinutes).toList())
                .containsExactly(5, 15, 30, 60);
        assertThat(scenarios).allSatisfy(scenario -> {
            assertThat(scenario.locale()).isEqualTo("fr-CM");
            assertThat(scenario.transcript()).isNotEmpty();
            assertThat(scenario.transcript().getLast().endOffsetMs())
                    .isLessThanOrEqualTo(scenario.durationMinutes() * 60_000L);
        });
    }

    @Test
    void shouldUseUniqueTranscriptAndFactKeys() {
        for (Scenario scenario : ClinicalBenchmarkCorpusV1.scenarios()) {
            Set<String> transcriptIds = new HashSet<>();
            scenario.transcript().forEach(turn ->
                    assertThat(transcriptIds.add(turn.id())).as(scenario.id() + " transcript " + turn.id()).isTrue());

            Set<String> factKeys = new HashSet<>();
            scenario.initialFacts().forEach(fact ->
                    assertThat(factKeys.add(fact.key())).as(scenario.id() + " initial " + fact.key()).isTrue());
            scenario.expectedEffectiveFacts().forEach(fact -> {
                if (scenario.initialFacts().stream().noneMatch(initial -> initial.key().equals(fact.key()))) {
                    assertThat(factKeys.add(fact.key())).as(scenario.id() + " expected " + fact.key()).isTrue();
                }
            });
        }
    }

    @Test
    void shouldCoverMedicationUnitsNegationLateralityAssessmentOrderAndHistory() {
        List<Scenario> scenarios = ClinicalBenchmarkCorpusV1.scenarios();

        Set<String> factTypes = new HashSet<>();
        Set<String> units = new HashSet<>();
        Set<String> polarities = new HashSet<>();
        Set<String> lateralities = new HashSet<>();
        scenarios.forEach(scenario -> {
            scenario.initialFacts().forEach(fact -> {
                factTypes.add(fact.factType());
                if (fact.unitCode() != null) units.add(fact.unitCode());
                polarities.add(fact.polarity());
                lateralities.add(fact.laterality());
            });
            scenario.expectedEffectiveFacts().forEach(fact -> {
                factTypes.add(fact.factType());
                if (fact.unitCode() != null) units.add(fact.unitCode());
                polarities.add(fact.polarity());
                lateralities.add(fact.laterality());
            });
        });

        assertThat(factTypes).contains("SYMPTOM", "VITAL", "MEDICATION", "ALLERGY", "HISTORY", "ASSESSMENT", "ORDER");
        assertThat(units).contains("MMHG", "MG", "G");
        assertThat(polarities).contains("POSITIVE", "NEGATIVE", "UNCERTAIN");
        assertThat(lateralities).contains("LEFT", "RIGHT", "UNSPECIFIED");
    }

    @Test
    void shouldCoverAllRollingOperationTypes() {
        Set<String> operationTypes = new HashSet<>();
        ClinicalBenchmarkCorpusV1.scenarios().forEach(scenario ->
                scenario.expectedOperations().forEach(operation -> operationTypes.add(operation.type())));

        assertThat(operationTypes).containsExactlyInAnyOrder("KEEP", "ADD", "REPLACE", "RETRACT");
    }

    @Test
    void shouldRequireReasonAndVerbatimEvidenceForEveryRetraction() {
        for (Scenario scenario : ClinicalBenchmarkCorpusV1.scenarios()) {
            for (Operation operation : scenario.expectedOperations()) {
                if (!"RETRACT".equals(operation.type())) continue;

                assertThat(operation.retractionReason()).isNotBlank();
                assertThat(operation.evidence()).isNotEmpty();
                operation.evidence().forEach(evidence -> {
                    var turn = scenario.transcript().stream()
                            .filter(candidate -> candidate.id().equals(evidence.transcriptTurnId()))
                            .findFirst()
                            .orElseThrow();
                    assertThat(turn.text()).contains(evidence.quoteText());
                });
            }
        }
    }
}
