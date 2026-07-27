package com.joprelys.backend.ai.benchmark;

import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.CandidateRun;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Evidence;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Fact;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Operation;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Scenario;
import com.joprelys.backend.ai.benchmark.ClinicalBenchmarkModel.Score;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class ClinicalBenchmarkScorer {

    private static final Set<String> DEFAULT_CRITICAL_TYPES = Set.of(
            "VITAL", "MEDICATION", "ALLERGY", "ASSESSMENT", "PLAN", "ORDER");

    Score score(Scenario scenario, CandidateRun candidate) {
        validateScenarioAndCandidate(scenario, candidate);

        Map<String, Fact> expectedByClinicalKey = uniqueByClinicalKey(scenario.expectedEffectiveFacts(), "gold");
        Map<String, Fact> candidateByClinicalKey = uniqueByClinicalKey(candidate.effectiveFacts(), "candidate");
        Set<String> expectedKeys = expectedByClinicalKey.keySet();
        Set<String> candidateKeys = candidateByClinicalKey.keySet();

        Set<String> factTruePositiveKeys = intersection(expectedKeys, candidateKeys);
        Set<String> factFalsePositiveKeys = difference(candidateKeys, expectedKeys);
        Set<String> factFalseNegativeKeys = difference(expectedKeys, candidateKeys);

        int missingCriticalFacts = (int) factFalseNegativeKeys.stream()
                .map(expectedByClinicalKey::get)
                .filter(Fact::critical)
                .count();
        int unsupportedCriticalClaims = (int) factFalsePositiveKeys.stream()
                .map(candidateByClinicalKey::get)
                .filter(fact -> isCriticalCandidate(fact, scenario.expectedEffectiveFacts()))
                .count();
        int criticalRelationErrors = countCriticalRelationErrors(
                scenario.expectedEffectiveFacts(),
                factFalsePositiveKeys.stream().map(candidateByClinicalKey::get).toList());
        int criticalEvidenceErrors = countCriticalEvidenceErrors(
                factTruePositiveKeys,
                expectedByClinicalKey,
                candidateByClinicalKey);

        EvidenceScore evidenceScore = evidenceScore(
                factTruePositiveKeys,
                expectedByClinicalKey,
                candidateByClinicalKey);

        Set<String> expectedOperationSignatures = uniqueOperationSignatures(
                scenario.expectedOperations(), "gold");
        Set<String> candidateOperationSignatures = uniqueOperationSignatures(
                candidate.operations(), "candidate");
        Set<String> operationTruePositives = intersection(
                expectedOperationSignatures, candidateOperationSignatures);
        Set<String> operationFalsePositives = difference(
                candidateOperationSignatures, expectedOperationSignatures);
        Set<String> operationFalseNegatives = difference(
                expectedOperationSignatures, candidateOperationSignatures);

        Map<String, Fact> initialByKey = scenario.initialFacts().stream()
                .collect(Collectors.toMap(Fact::key, fact -> fact, (left, right) -> {
                    throw new IllegalArgumentException("duplicate initial fact key: " + left.key());
                }));
        int falseCriticalRetracts = falseCriticalMutationCount(
                candidate.operations(),
                expectedOperationSignatures,
                initialByKey,
                "RETRACT");
        int falseCriticalReplaces = falseCriticalMutationCount(
                candidate.operations(),
                expectedOperationSignatures,
                initialByKey,
                "REPLACE");

        boolean gatePassed = missingCriticalFacts == 0
                && unsupportedCriticalClaims == 0
                && criticalRelationErrors == 0
                && criticalEvidenceErrors == 0
                && falseCriticalRetracts == 0
                && falseCriticalReplaces == 0;

        return new Score(
                scenario.id(),
                candidate.pipeline(),
                expectedKeys.size(),
                candidateKeys.size(),
                factTruePositiveKeys.size(),
                factFalsePositiveKeys.size(),
                factFalseNegativeKeys.size(),
                ratio(factTruePositiveKeys.size(), candidateKeys.size()),
                ratio(factTruePositiveKeys.size(), expectedKeys.size()),
                missingCriticalFacts,
                unsupportedCriticalClaims,
                criticalRelationErrors,
                criticalEvidenceErrors,
                evidenceScore.expectedCount(),
                evidenceScore.candidateCount(),
                evidenceScore.supportedCount(),
                evidenceScore.recoveredCount(),
                ratio(evidenceScore.supportedCount(), evidenceScore.candidateCount()),
                ratio(evidenceScore.recoveredCount(), evidenceScore.expectedCount()),
                expectedOperationSignatures.size(),
                candidateOperationSignatures.size(),
                operationTruePositives.size(),
                operationFalsePositives.size(),
                operationFalseNegatives.size(),
                ratio(operationTruePositives.size(), candidateOperationSignatures.size()),
                ratio(operationTruePositives.size(), expectedOperationSignatures.size()),
                falseCriticalRetracts,
                falseCriticalReplaces,
                gatePassed);
    }

    private EvidenceScore evidenceScore(
            Set<String> matchedClinicalKeys,
            Map<String, Fact> expectedByClinicalKey,
            Map<String, Fact> candidateByClinicalKey) {
        int expected = expectedByClinicalKey.values().stream()
                .mapToInt(fact -> evidenceKeys(fact.evidence()).size())
                .sum();
        int candidate = candidateByClinicalKey.values().stream()
                .mapToInt(fact -> evidenceKeys(fact.evidence()).size())
                .sum();
        int matched = 0;
        for (String key : matchedClinicalKeys) {
            Set<String> expectedEvidence = evidenceKeys(expectedByClinicalKey.get(key).evidence());
            Set<String> candidateEvidence = evidenceKeys(candidateByClinicalKey.get(key).evidence());
            matched += intersection(expectedEvidence, candidateEvidence).size();
        }
        return new EvidenceScore(expected, candidate, matched, matched);
    }

    private int countCriticalEvidenceErrors(
            Set<String> matchedClinicalKeys,
            Map<String, Fact> expectedByClinicalKey,
            Map<String, Fact> candidateByClinicalKey) {
        int errors = 0;
        for (String key : matchedClinicalKeys) {
            Fact expected = expectedByClinicalKey.get(key);
            if (!expected.critical()) continue;
            Set<String> expectedEvidence = evidenceKeys(expected.evidence());
            Set<String> candidateEvidence = evidenceKeys(candidateByClinicalKey.get(key).evidence());
            if (!expectedEvidence.equals(candidateEvidence)) errors++;
        }
        return errors;
    }

    private int countCriticalRelationErrors(List<Fact> expected, List<Fact> falsePositives) {
        Map<String, List<Fact>> expectedByConcept = expected.stream()
                .filter(Fact::critical)
                .collect(Collectors.groupingBy(fact -> normalize(fact.conceptCode())));
        int errors = 0;
        for (Fact candidate : falsePositives) {
            List<Fact> sameConcept = expectedByConcept.getOrDefault(
                    normalize(candidate.conceptCode()), List.of());
            if (!sameConcept.isEmpty()) errors++;
        }
        return errors;
    }

    private int falseCriticalMutationCount(
            List<Operation> candidateOperations,
            Set<String> expectedSignatures,
            Map<String, Fact> initialByKey,
            String operationType) {
        int count = 0;
        for (Operation operation : candidateOperations) {
            if (!operationType.equals(normalizeUpper(operation.type()))) continue;
            Fact target = initialByKey.get(operation.targetFactKey());
            if (target == null || !target.critical()) continue;
            if (!expectedSignatures.contains(operationSignature(operation))) count++;
        }
        return count;
    }

    private boolean isCriticalCandidate(Fact fact, List<Fact> expectedFacts) {
        if (fact.critical()) return true;
        if (DEFAULT_CRITICAL_TYPES.contains(normalizeUpper(fact.factType()))) return true;
        if (fact.valuePrimary() != null || fact.valueSecondary() != null || fact.unitCode() != null) return true;
        return expectedFacts.stream()
                .filter(Fact::critical)
                .anyMatch(expected -> Objects.equals(
                        normalize(expected.conceptCode()), normalize(fact.conceptCode())));
    }

    private Map<String, Fact> uniqueByClinicalKey(List<Fact> facts, String source) {
        Map<String, Fact> result = new HashMap<>();
        for (Fact fact : facts) {
            String key = clinicalKey(fact);
            if (result.put(key, fact) != null) {
                throw new IllegalArgumentException("duplicate " + source + " clinical fact: " + key);
            }
        }
        return Map.copyOf(result);
    }

    private Set<String> uniqueOperationSignatures(List<Operation> operations, String source) {
        Set<String> signatures = new HashSet<>();
        Set<String> targets = new HashSet<>();
        for (Operation operation : operations) {
            String signature = operationSignature(operation);
            if (!signatures.add(signature)) {
                throw new IllegalArgumentException("duplicate " + source + " operation: " + signature);
            }
            if (operation.targetFactKey() != null && !targets.add(operation.targetFactKey())) {
                throw new IllegalArgumentException(
                        "duplicate " + source + " operation target: " + operation.targetFactKey());
            }
        }
        return Set.copyOf(signatures);
    }

    private String operationSignature(Operation operation) {
        require(operation, "operation");
        String type = normalizeUpper(operation.type());
        return switch (type) {
            case "KEEP" -> "KEEP|" + require(operation.targetFactKey(), "targetFactKey");
            case "ADD" -> "ADD|" + factOperationKey(require(operation.resultFact(), "resultFact"));
            case "REPLACE" -> "REPLACE|" + require(operation.targetFactKey(), "targetFactKey")
                    + "|" + factOperationKey(require(operation.resultFact(), "resultFact"));
            case "RETRACT" -> "RETRACT|" + require(operation.targetFactKey(), "targetFactKey")
                    + "|" + normalizeUpper(require(operation.retractionReason(), "retractionReason"))
                    + "|" + orderedEvidenceKey(operation.evidence());
            default -> throw new IllegalArgumentException("unsupported operation type: " + operation.type());
        };
    }

    private String factOperationKey(Fact fact) {
        return clinicalKey(fact) + "|" + orderedEvidenceKey(fact.evidence());
    }

    private String clinicalKey(Fact fact) {
        require(fact, "fact");
        return String.join("|",
                normalizeUpper(fact.factType()),
                normalizeUpper(fact.authority()),
                normalizeUpper(fact.conceptCode()),
                normalize(fact.conceptText()),
                normalizeUpper(fact.polarity()),
                normalize(fact.valuePrimary()),
                normalize(fact.valueSecondary()),
                normalizeUpper(fact.unitCode()),
                normalize(fact.temporalityText()),
                normalizeUpper(fact.laterality()),
                normalize(fact.frequencyText()),
                normalize(fact.routeText()));
    }

    private String orderedEvidenceKey(List<Evidence> evidence) {
        List<String> keys = new ArrayList<>(evidenceKeys(evidence));
        keys.sort(String::compareTo);
        return String.join(";", keys);
    }

    private Set<String> evidenceKeys(List<Evidence> evidence) {
        if (evidence == null) return Set.of();
        Set<String> result = new HashSet<>();
        for (Evidence item : evidence) {
            String key = require(item.transcriptTurnId(), "transcriptTurnId")
                    + "|" + require(item.quoteText(), "quoteText");
            if (!result.add(key)) {
                throw new IllegalArgumentException("duplicate evidence: " + key);
            }
        }
        return Set.copyOf(result);
    }

    private void validateScenarioAndCandidate(Scenario scenario, CandidateRun candidate) {
        require(scenario, "scenario");
        require(candidate, "candidate");
        if (!scenario.id().equals(candidate.scenarioId())) {
            throw new IllegalArgumentException("candidate scenario mismatch");
        }
        if (candidate.pipeline() == null || candidate.effectiveFacts() == null || candidate.operations() == null) {
            throw new IllegalArgumentException("candidate run incomplete");
        }
        if (scenario.expectedEffectiveFacts() == null || scenario.expectedOperations() == null
                || scenario.initialFacts() == null || scenario.transcript() == null) {
            throw new IllegalArgumentException("scenario gold incomplete");
        }
        validateEvidenceAgainstTranscript(scenario, scenario.expectedEffectiveFacts(), scenario.expectedOperations());
        validateEvidenceAgainstTranscript(scenario, candidate.effectiveFacts(), candidate.operations());
    }

    private void validateEvidenceAgainstTranscript(
            Scenario scenario,
            List<Fact> facts,
            List<Operation> operations) {
        Map<String, TranscriptText> transcript = scenario.transcript().stream()
                .collect(Collectors.toMap(
                        turn -> require(turn.id(), "transcriptTurnId"),
                        turn -> new TranscriptText(require(turn.text(), "transcriptText")),
                        (left, right) -> {
                            throw new IllegalArgumentException("duplicate transcript turn id");
                        }));
        for (Fact fact : facts) validateEvidenceList(fact.evidence(), transcript);
        for (Operation operation : operations) {
            validateEvidenceList(operation.evidence(), transcript);
            if (operation.resultFact() != null) validateEvidenceList(operation.resultFact().evidence(), transcript);
        }
    }

    private void validateEvidenceList(List<Evidence> evidence, Map<String, TranscriptText> transcript) {
        if (evidence == null) return;
        for (Evidence item : evidence) {
            TranscriptText text = transcript.get(require(item.transcriptTurnId(), "transcriptTurnId"));
            if (text == null || !text.value().contains(require(item.quoteText(), "quoteText"))) {
                throw new IllegalArgumentException("evidence is not verbatim from transcript");
            }
        }
    }

    private Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private double ratio(int numerator, int denominator) {
        return denominator == 0 ? 1.0d : ((double) numerator) / denominator;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private <T> T require(T value, String name) {
        if (value == null || (value instanceof String string && string.isBlank())) {
            throw new IllegalArgumentException(name + " required");
        }
        return value;
    }

    private record EvidenceScore(
            int expectedCount,
            int candidateCount,
            int supportedCount,
            int recoveredCount) {
    }

    private record TranscriptText(String value) {
    }
}
