package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ClinicalFactRevisionRequestHasher {

    public String sha256(ApplyRevisionRequest request) {
        StringBuilder canonical = new StringBuilder(1024);
        append(canonical, request.revisionId());
        append(canonical, normalize(request.baseProjectionVersion()));
        if (request.operations() != null) {
            for (int index = 0; index < request.operations().size(); index++) {
                RevisionOperationRequest operation = request.operations().get(index);
                append(canonical, index);
                if (operation == null) {
                    append(canonical, "NULL_OPERATION");
                    continue;
                }
                append(canonical, operation.operationId());
                append(canonical, operation.type());
                append(canonical, operation.targetFactId());
                appendFact(canonical, operation.fact());
                appendRetraction(canonical, operation.retraction());
            }
        }
        return digest(canonical.toString());
    }

    private void appendFact(StringBuilder canonical, FactPayload fact) {
        if (fact == null) {
            append(canonical, "NULL_FACT");
            return;
        }
        append(canonical, fact.factType());
        append(canonical, fact.authority());
        append(canonical, upper(fact.conceptCode()));
        append(canonical, normalize(fact.conceptText()));
        append(canonical, fact.polarity());
        append(canonical, normalize(fact.valuePrimary()));
        append(canonical, normalize(fact.valueSecondary()));
        append(canonical, upper(fact.unitCode()));
        append(canonical, normalize(fact.temporalityText()));
        append(canonical, fact.laterality());
        append(canonical, normalize(fact.frequencyText()));
        append(canonical, normalize(fact.routeText()));
        appendEvidence(canonical, fact.evidence());
    }

    private void appendRetraction(StringBuilder canonical, RetractionPayload retraction) {
        if (retraction == null) {
            append(canonical, "NULL_RETRACTION");
            return;
        }
        append(canonical, retraction.authority());
        append(canonical, retraction.reason());
        appendEvidence(canonical, retraction.evidence());
    }

    private void appendEvidence(StringBuilder canonical, List<EvidenceSpanCandidate> evidence) {
        if (evidence == null) {
            append(canonical, "NULL_EVIDENCE");
            return;
        }
        List<EvidenceSpanCandidate> sorted = new ArrayList<>(evidence);
        sorted.sort(Comparator.nullsFirst(
                Comparator.comparing(EvidenceSpanCandidate::transcriptItemId,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparingInt(EvidenceSpanCandidate::quoteStartChar)
                        .thenComparingInt(EvidenceSpanCandidate::quoteEndChar)
                        .thenComparing(EvidenceSpanCandidate::quoteText,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(EvidenceSpanCandidate::primarySupport)));
        append(canonical, sorted.size());
        for (EvidenceSpanCandidate span : sorted) {
            if (span == null) {
                append(canonical, "NULL_SPAN");
                continue;
            }
            append(canonical, span.transcriptItemId());
            append(canonical, span.quoteStartChar());
            append(canonical, span.quoteEndChar());
            append(canonical, span.quoteText());
            append(canonical, span.primarySupport());
        }
    }

    private void append(StringBuilder canonical, Object value) {
        String text = value == null ? "<null>" : value.toString();
        canonical.append(text.length()).append(':').append(text).append('|');
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String upper(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? "" : normalized.toUpperCase(Locale.ROOT);
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
