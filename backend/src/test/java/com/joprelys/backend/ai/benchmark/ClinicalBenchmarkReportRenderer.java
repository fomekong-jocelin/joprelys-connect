package com.joprelys.backend.ai.benchmark;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Score;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class ClinicalBenchmarkReportRenderer {

    String markdown(String corpusVersion, List<Score> scores) {
        List<Score> ordered = ordered(scores);
        StringBuilder out = new StringBuilder();
        out.append("# Clinical AI Benchmark\n\n");
        out.append("Corpus: `").append(corpusVersion).append("`\n\n");
        out.append("Safety decision: **")
                .append(allSafetyGatesPassed(ordered) ? "PASS" : "FAIL")
                .append("**\n\n");
        out.append("| Scenario | Pipeline | Fact P | Fact R | Evidence P | Evidence R | Op P | Op R | Missing critical | Unsupported critical | Relation errors | Evidence errors | False RETRACT | False REPLACE | Gate |\n");
        out.append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
        for (Score score : ordered) {
            out.append("|")
                    .append(score.scenarioId()).append("|")
                    .append(score.pipeline()).append("|")
                    .append(percent(score.factPrecision())).append("|")
                    .append(percent(score.factRecall())).append("|")
                    .append(percent(score.evidencePrecision())).append("|")
                    .append(percent(score.evidenceRecall())).append("|")
                    .append(percent(score.operationPrecision())).append("|")
                    .append(percent(score.operationRecall())).append("|")
                    .append(score.missingCriticalFacts()).append("|")
                    .append(score.unsupportedCriticalClaims()).append("|")
                    .append(score.criticalRelationErrors()).append("|")
                    .append(score.criticalEvidenceErrors()).append("|")
                    .append(score.falseCriticalRetracts()).append("|")
                    .append(score.falseCriticalReplaces()).append("|")
                    .append(score.absoluteSafetyGatePassed() ? "PASS" : "FAIL")
                    .append("|\n");
        }
        return out.toString();
    }

    String json(String corpusVersion, List<Score> scores) {
        List<Score> ordered = ordered(scores);
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"corpusVersion\": \"").append(escape(corpusVersion)).append("\",\n");
        out.append("  \"allSafetyGatesPassed\": ").append(allSafetyGatesPassed(ordered)).append(",\n");
        out.append("  \"scores\": [\n");
        for (int i = 0; i < ordered.size(); i++) {
            Score score = ordered.get(i);
            out.append("    {")
                    .append("\"scenarioId\":\"").append(escape(score.scenarioId())).append("\",")
                    .append("\"pipeline\":\"").append(score.pipeline()).append("\",")
                    .append("\"factPrecision\":").append(decimal(score.factPrecision())).append(",")
                    .append("\"factRecall\":").append(decimal(score.factRecall())).append(",")
                    .append("\"evidencePrecision\":").append(decimal(score.evidencePrecision())).append(",")
                    .append("\"evidenceRecall\":").append(decimal(score.evidenceRecall())).append(",")
                    .append("\"operationPrecision\":").append(decimal(score.operationPrecision())).append(",")
                    .append("\"operationRecall\":").append(decimal(score.operationRecall())).append(",")
                    .append("\"missingCriticalFacts\":").append(score.missingCriticalFacts()).append(",")
                    .append("\"unsupportedCriticalClaims\":").append(score.unsupportedCriticalClaims()).append(",")
                    .append("\"criticalRelationErrors\":").append(score.criticalRelationErrors()).append(",")
                    .append("\"criticalEvidenceErrors\":").append(score.criticalEvidenceErrors()).append(",")
                    .append("\"falseCriticalRetracts\":").append(score.falseCriticalRetracts()).append(",")
                    .append("\"falseCriticalReplaces\":").append(score.falseCriticalReplaces()).append(",")
                    .append("\"absoluteSafetyGatePassed\":").append(score.absoluteSafetyGatePassed())
                    .append("}");
            if (i + 1 < ordered.size()) out.append(",");
            out.append("\n");
        }
        out.append("  ]\n");
        out.append("}\n");
        return out.toString();
    }

    private boolean allSafetyGatesPassed(List<Score> scores) {
        return !scores.isEmpty() && scores.stream().allMatch(Score::absoluteSafetyGatePassed);
    }

    private List<Score> ordered(List<Score> scores) {
        if (scores == null) throw new IllegalArgumentException("scores required");
        return scores.stream()
                .sorted(Comparator.comparing(Score::scenarioId)
                        .thenComparing(score -> score.pipeline().name()))
                .toList();
    }

    private String percent(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0d);
    }

    private String decimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
