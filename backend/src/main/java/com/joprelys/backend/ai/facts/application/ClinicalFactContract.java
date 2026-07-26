package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClinicalFactContract {

    private ClinicalFactContract() {
    }

    public record EvidenceSpanCandidate(
            UUID transcriptItemId,
            int quoteStartChar,
            int quoteEndChar,
            String quoteText,
            boolean primarySupport) {
    }

    public record FactCandidate(
            String sourceEventId,
            FactType factType,
            Authority authority,
            String conceptCode,
            String conceptText,
            Polarity polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporalityText,
            Laterality laterality,
            String frequencyText,
            String routeText,
            FactStatus status,
            UUID supersedesFactId,
            List<EvidenceSpanCandidate> evidence) {
    }

    public record EvidenceSpanView(
            UUID id,
            UUID transcriptItemId,
            int quoteStartChar,
            int quoteEndChar,
            String quoteText,
            boolean primarySupport) {
    }

    public record FactView(
            UUID id,
            long sequence,
            String sourceEventId,
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
            String status,
            UUID supersedesFactId,
            Instant createdAt,
            List<EvidenceSpanView> evidence) {
    }

    public record FactLedgerView(UUID visitId, List<FactView> facts) {
    }
}
