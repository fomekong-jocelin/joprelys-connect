package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionRequestHasherTest {

    private final ClinicalFactRevisionRequestHasher hasher = new ClinicalFactRevisionRequestHasher();

    @Test
    void shouldProduceSameHashWhenEquivalentEvidenceOrderChanges() {
        UUID revisionId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        EvidenceSpanCandidate first = evidence(UUID.randomUUID(), "douleur abdominale", true);
        EvidenceSpanCandidate second = evidence(UUID.randomUUID(), "depuis trois jours", false);

        ApplyRevisionRequest left = request(
                revisionId,
                operationId,
                payload("douleur abdominale", List.of(first, second)));
        ApplyRevisionRequest right = request(
                revisionId,
                operationId,
                payload("douleur abdominale", List.of(second, first)));

        assertThat(hasher.sha256(left)).isEqualTo(hasher.sha256(right));
        assertThat(hasher.sha256(left)).matches("[0-9a-f]{64}");
    }

    @Test
    void shouldChangeHashWhenClinicalPayloadChanges() {
        UUID revisionId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();

        ApplyRevisionRequest original = request(
                revisionId,
                operationId,
                payload("douleur abdominale", List.of(evidence(
                        transcriptItemId, "douleur abdominale", true))));
        ApplyRevisionRequest changed = request(
                revisionId,
                operationId,
                payload("douleur thoracique", List.of(evidence(
                        transcriptItemId, "douleur thoracique", true))));

        assertThat(hasher.sha256(changed)).isNotEqualTo(hasher.sha256(original));
    }

    private ApplyRevisionRequest request(
            UUID revisionId,
            UUID operationId,
            FactPayload payload) {
        return new ApplyRevisionRequest(
                revisionId,
                "clinical-note-projection-v1:base",
                List.of(new RevisionOperationRequest(
                        operationId,
                        OperationType.ADD,
                        null,
                        payload,
                        null)));
    }

    private FactPayload payload(String concept, List<EvidenceSpanCandidate> evidence) {
        return new FactPayload(
                FactType.SYMPTOM,
                Authority.PATIENT_REPORTED,
                "SYMPTOM_TEST",
                concept,
                Polarity.POSITIVE,
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED,
                null,
                null,
                evidence);
    }

    private EvidenceSpanCandidate evidence(
            UUID transcriptItemId,
            String quote,
            boolean primary) {
        return new EvidenceSpanCandidate(
                transcriptItemId,
                0,
                quote.length(),
                quote,
                primary);
    }
}
