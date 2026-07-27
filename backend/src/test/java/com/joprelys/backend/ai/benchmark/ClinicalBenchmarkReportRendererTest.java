package com.joprelys.backend.ai.benchmark;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.CandidateRun;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Pipeline;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Scenario;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Score;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClinicalBenchmarkReportRendererTest {

    private final ClinicalBenchmarkScorer scorer = new ClinicalBenchmarkScorer();
    private final ClinicalBenchmarkReportRenderer renderer = new ClinicalBenchmarkReportRenderer();

    @Test
    void shouldRenderSameSafetyColumnsForBothPipelines() {
        List<Score> scores = new ArrayList<>();
        for (Scenario scenario : ClinicalBenchmarkCorpusV1.scenarios()) {
            scores.add(scorer.score(scenario, goldRun(scenario, Pipeline.FACT_EXTRACTION_196)));
            scores.add(scorer.score(scenario, goldRun(scenario, Pipeline.ROLLING_PLANNER_211)));
        }

        String markdown = renderer.markdown(ClinicalBenchmarkCorpusV1.VERSION, scores);
        String json = renderer.json(ClinicalBenchmarkCorpusV1.VERSION, scores);

        assertThat(markdown).contains("Safety decision: **PASS**");
        assertThat(markdown).contains("FACT_EXTRACTION_196", "ROLLING_PLANNER_211");
        assertThat(markdown).contains("Unsupported critical", "Evidence errors", "False RETRACT", "False REPLACE");
        assertThat(json).contains("\"allSafetyGatesPassed\": true");
        assertThat(json).contains("\"criticalEvidenceErrors\":0");
        assertThat(json).contains("\"absoluteSafetyGatePassed\":true");
    }

    @Test
    void shouldExposeFailWithoutHidingCriticalOmissionBehindOtherPerfectScenarios() {
        List<Score> scores = new ArrayList<>();
        for (Scenario scenario : ClinicalBenchmarkCorpusV1.scenarios()) {
            scores.add(scorer.score(scenario, goldRun(scenario, Pipeline.ROLLING_PLANNER_211)));
        }
        Scenario fiveMinutes = ClinicalBenchmarkCorpusV1.scenarios().getFirst();
        var withoutCriticalBp = fiveMinutes.expectedEffectiveFacts().stream()
                .filter(fact -> !"BP".equals(fact.conceptCode()))
                .toList();
        scores.set(0, scorer.score(
                fiveMinutes,
                new CandidateRun(
                        fiveMinutes.id(),
                        Pipeline.ROLLING_PLANNER_211,
                        "candidate-fixture",
                        ClinicalBenchmarkCorpusV1.VERSION,
                        withoutCriticalBp,
                        fiveMinutes.expectedOperations())));

        String markdown = renderer.markdown(ClinicalBenchmarkCorpusV1.VERSION, scores);
        String json = renderer.json(ClinicalBenchmarkCorpusV1.VERSION, scores);

        assertThat(markdown).contains("Safety decision: **FAIL**");
        assertThat(json).contains("\"allSafetyGatesPassed\": false");
        assertThat(json).contains("\"missingCriticalFacts\":1");
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
}
