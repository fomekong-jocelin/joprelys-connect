package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClinicalFactRevisionContract {

    private ClinicalFactRevisionContract() {
    }

    public enum OperationType {
        KEEP,
        ADD,
        REPLACE,
        RETRACT
    }

    public enum RetractionReason {
        EXPLICIT_CORRECTION,
        EXPLICIT_NEGATION,
        CLINICIAN_CANCELLATION
    }

    public record FactPayload(
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
            List<EvidenceSpanCandidate> evidence) {
    }

    public record RetractionPayload(
            Authority authority,
            RetractionReason reason,
            List<EvidenceSpanCandidate> evidence) {
    }

    public record RevisionOperationRequest(
            UUID operationId,
            OperationType type,
            UUID targetFactId,
            FactPayload fact,
            RetractionPayload retraction) {
    }

    public record ApplyRevisionRequest(
            UUID revisionId,
            String baseProjectionVersion,
            List<RevisionOperationRequest> operations) {
    }

    public record RevisionOperationView(
            UUID operationId,
            int position,
            String type,
            UUID targetFactId,
            UUID resultFactId,
            String retractionReason,
            List<EvidenceSpanCandidate> evidence) {
    }

    public record RevisionBatchView(
            UUID id,
            UUID revisionId,
            UUID visitId,
            String baseProjectionVersion,
            String resultProjectionVersion,
            UUID createdByUserId,
            Instant createdAt,
            List<RevisionOperationView> operations) {
    }

    public record RevisionHistoryView(
            UUID visitId,
            List<RevisionBatchView> revisions) {
    }
}
