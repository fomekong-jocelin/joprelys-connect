package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ClinicalFactRevisionSemanticGuard {

    private ClinicalFactRevisionSemanticGuard() {
    }

    static boolean sameFact(FactView current, FactPayload proposed) {
        if (current == null || proposed == null
                || proposed.factType() == null
                || proposed.authority() == null
                || proposed.polarity() == null
                || proposed.laterality() == null) {
            return false;
        }
        if (!current.factType().equals(proposed.factType().name())
                || !current.authority().equals(proposed.authority().name())
                || !Objects.equals(current.conceptCode(), upper(proposed.conceptCode()))
                || !Objects.equals(current.conceptText(), trim(proposed.conceptText()))
                || !current.polarity().equals(proposed.polarity().name())
                || !Objects.equals(current.valuePrimary(), trim(proposed.valuePrimary()))
                || !Objects.equals(current.valueSecondary(), trim(proposed.valueSecondary()))
                || !Objects.equals(current.unitCode(), upper(proposed.unitCode()))
                || !Objects.equals(current.temporalityText(), trim(proposed.temporalityText()))
                || !current.laterality().equals(proposed.laterality().name())
                || !Objects.equals(current.frequencyText(), trim(proposed.frequencyText()))
                || !Objects.equals(current.routeText(), trim(proposed.routeText()))) {
            return false;
        }
        if (current.evidence() == null || proposed.evidence() == null
                || current.evidence().size() != proposed.evidence().size()) {
            return false;
        }
        List<String> currentEvidence = current.evidence().stream()
                .map(ClinicalFactRevisionSemanticGuard::key)
                .sorted()
                .toList();
        return currentEvidence.equals(candidateEvidenceKeys(proposed.evidence()));
    }

    static boolean samePayload(FactPayload left, FactPayload right) {
        if (left == null || right == null
                || left.factType() != right.factType()
                || left.authority() != right.authority()
                || left.polarity() != right.polarity()
                || left.laterality() != right.laterality()
                || !Objects.equals(upper(left.conceptCode()), upper(right.conceptCode()))
                || !Objects.equals(trim(left.conceptText()), trim(right.conceptText()))
                || !Objects.equals(trim(left.valuePrimary()), trim(right.valuePrimary()))
                || !Objects.equals(trim(left.valueSecondary()), trim(right.valueSecondary()))
                || !Objects.equals(upper(left.unitCode()), upper(right.unitCode()))
                || !Objects.equals(trim(left.temporalityText()), trim(right.temporalityText()))
                || !Objects.equals(trim(left.frequencyText()), trim(right.frequencyText()))
                || !Objects.equals(trim(left.routeText()), trim(right.routeText()))) {
            return false;
        }
        if (left.evidence() == null || right.evidence() == null
                || left.evidence().size() != right.evidence().size()) {
            return false;
        }
        return candidateEvidenceKeys(left.evidence()).equals(candidateEvidenceKeys(right.evidence()));
    }

    private static List<String> candidateEvidenceKeys(List<EvidenceSpanCandidate> evidence) {
        return evidence.stream()
                .map(ClinicalFactRevisionSemanticGuard::key)
                .sorted()
                .toList();
    }

    private static String key(EvidenceSpanView evidence) {
        if (evidence == null) return "<null>";
        return evidence.transcriptItemId()
                + "|" + evidence.quoteStartChar()
                + "|" + evidence.quoteEndChar()
                + "|" + evidence.quoteText()
                + "|" + evidence.primarySupport();
    }

    private static String key(EvidenceSpanCandidate evidence) {
        if (evidence == null) return "<null>";
        return evidence.transcriptItemId()
                + "|" + evidence.quoteStartChar()
                + "|" + evidence.quoteEndChar()
                + "|" + evidence.quoteText()
                + "|" + evidence.primarySupport();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String upper(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
}
