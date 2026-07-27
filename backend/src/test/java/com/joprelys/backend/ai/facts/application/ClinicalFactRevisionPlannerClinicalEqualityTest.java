package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEnvelope;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEvidence;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerFact;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerOperation;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClinicalFactRevisionPlannerClinicalEqualityTest {

    private final ClinicalFactRevisionPlanNormalizer normalizer = new ClinicalFactRevisionPlanNormalizer(
            mock(ClinicalFactEvidenceValidator.class),
            mock(ClinicalFactRetractionValidator.class));

    @Test
    void shouldNormalizeRestatedAddToKeepEvenWhenNewTranscriptProvidesDifferentEvidence() {
        UUID oldItemId = UUID.randomUUID();
        TranscriptItemView newItem = transcript("douleur abdominale");
        FactView existing = fact(UUID.randomUUID(), oldItemId);
        PlannerFact restated = plannerFact(newItem);

        var operations = normalizer.normalize(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(existing),
                Map.of(newItem.id(), newItem),
                new PlannerEnvelope(List.of(new PlannerOperation(
                        "ADD", null, restated, null))));

        assertThat(operations).hasSize(1);
        assertThat(operations.getFirst().type()).isEqualTo(OperationType.KEEP);
        assertThat(operations.getFirst().targetFactId()).isEqualTo(existing.id());
    }

    @Test
    void shouldNormalizeRestatedReplacementToKeepEvenWhenEvidenceChanged() {
        UUID oldItemId = UUID.randomUUID();
        TranscriptItemView newItem = transcript("douleur abdominale");
        FactView existing = fact(UUID.randomUUID(), oldItemId);
        PlannerFact restated = plannerFact(newItem);

        var operations = normalizer.normalize(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(existing),
                Map.of(newItem.id(), newItem),
                new PlannerEnvelope(List.of(new PlannerOperation(
                        "REPLACE", existing.id().toString(), restated, null))));

        assertThat(operations).hasSize(1);
        assertThat(operations.getFirst().type()).isEqualTo(OperationType.KEEP);
        assertThat(operations.getFirst().targetFactId()).isEqualTo(existing.id());
    }

    private PlannerFact plannerFact(TranscriptItemView item) {
        return new PlannerFact(
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                Polarity.POSITIVE.name(),
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED.name(),
                null,
                null,
                List.of(new PlannerEvidence(
                        item.id().toString(),
                        "douleur abdominale",
                        true)));
    }

    private FactView fact(UUID id, UUID transcriptItemId) {
        return new FactView(
                id,
                1,
                "existing-" + id,
                FactType.SYMPTOM.name(),
                Authority.PATIENT_REPORTED.name(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                Polarity.POSITIVE.name(),
                null,
                null,
                null,
                null,
                Laterality.UNSPECIFIED.name(),
                null,
                null,
                FactStatus.ASSERTED.name(),
                null,
                Instant.now(),
                List.of(new EvidenceSpanView(
                        UUID.randomUUID(),
                        transcriptItemId,
                        0,
                        "douleur abdominale".length(),
                        "douleur abdominale",
                        true)));
    }

    private TranscriptItemView transcript(String text) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                8,
                "planner-restatement",
                "AMBIENT_DIARIZED",
                "PATIENT",
                "patient",
                text,
                "fr",
                8_000,
                10_000,
                "FINAL",
                null,
                Instant.now());
    }
}
