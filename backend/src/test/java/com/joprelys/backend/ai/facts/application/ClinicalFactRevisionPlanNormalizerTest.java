package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEnvelope;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEvidence;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerFact;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerOperation;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerRetraction;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClinicalFactRevisionPlanNormalizerTest {

    private final ClinicalFactEvidenceValidator evidenceValidator = mock(ClinicalFactEvidenceValidator.class);
    private final ClinicalFactRetractionValidator retractionValidator = mock(ClinicalFactRetractionValidator.class);
    private final ClinicalFactRevisionPlanNormalizer normalizer =
            new ClinicalFactRevisionPlanNormalizer(evidenceValidator, retractionValidator);

    private UUID visitId;
    private UUID organizationId;
    private UUID revisionId;
    private TranscriptItemView patientItem;
    private Map<UUID, TranscriptItemView> allowedItems;

    @BeforeEach
    void setUp() {
        visitId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        revisionId = UUID.randomUUID();
        patientItem = transcript("douleur abdominale depuis trois jours", "PATIENT");
        allowedItems = new LinkedHashMap<>();
        allowedItems.put(patientItem.id(), patientItem);
    }

    @Test
    void shouldKeepAnExistingEffectiveFactWithoutInventingPayload() {
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "KEEP", existing.id().toString(), null, null)));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(OperationType.KEEP);
        assertThat(result.getFirst().targetFactId()).isEqualTo(existing.id());
        assertThat(result.getFirst().fact()).isNull();
    }

    @Test
    void shouldCreateAddWithOffsetsRecomputedFromExactQuote() {
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(add(
                factOutput("ABDOMINAL_PAIN", "douleur abdominale", "douleur abdominale"))));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(), allowedItems, envelope);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().type()).isEqualTo(OperationType.ADD);
        var evidence = result.getFirst().fact().evidence().getFirst();
        assertThat(evidence.transcriptItemId()).isEqualTo(patientItem.id());
        assertThat(evidence.quoteStartChar()).isZero();
        assertThat(evidence.quoteEndChar()).isEqualTo("douleur abdominale".length());
        verify(evidenceValidator).validate(eq(visitId), eq(organizationId), any());
    }

    @Test
    void shouldNormalizeExactDuplicateAddToKeep() {
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(add(
                factOutput("ABDOMINAL_PAIN", "douleur abdominale", "douleur abdominale"))));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope);

        assertThat(result.getFirst().type()).isEqualTo(OperationType.KEEP);
        assertThat(result.getFirst().targetFactId()).isEqualTo(existing.id());
    }

    @Test
    void shouldReplaceOnlyWithinSameClinicalLineage() {
        TranscriptItemView doctorItem = transcript("douleur abdominale depuis quatre jours", "DOCTOR");
        allowedItems.put(doctorItem.id(), doctorItem);
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                doctorItem.id(),
                Authority.CLINICIAN_OBSERVED);
        PlannerFact replacement = factOutput(
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale depuis quatre jours",
                doctorItem,
                Authority.CLINICIAN_OBSERVED,
                "depuis quatre jours");
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "REPLACE", existing.id().toString(), replacement, null)));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope);

        assertThat(result.getFirst().type()).isEqualTo(OperationType.REPLACE);
        assertThat(result.getFirst().targetFactId()).isEqualTo(existing.id());
    }

    @Test
    void shouldRejectReplacementThatChangesClinicalConceptLineage() {
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "REPLACE",
                existing.id().toString(),
                factOutput("CHEST_PAIN", "douleur abdominale", "douleur abdominale"),
                null)));

        assertThatThrownBy(() -> normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_REPLACE_LINEAGE_INVALID");
    }

    @Test
    void shouldNormalizeNoOpReplacementToKeep() {
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "REPLACE",
                existing.id().toString(),
                factOutput("ABDOMINAL_PAIN", "douleur abdominale", "douleur abdominale"),
                null)));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope);

        assertThat(result.getFirst().type()).isEqualTo(OperationType.KEEP);
    }

    @Test
    void shouldDryRunRetractionAgainstExistingValidator() {
        TranscriptItemView correction = transcript("je n'ai pas de douleur abdominale", "PATIENT");
        allowedItems.put(correction.id(), correction);
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerRetraction retraction = new PlannerRetraction(
                "PATIENT_REPORTED",
                "EXPLICIT_NEGATION",
                List.of(new PlannerEvidence(
                        correction.id().toString(), correction.text(), true)));
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "RETRACT", existing.id().toString(), null, retraction)));

        var result = normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope);

        assertThat(result.getFirst().type()).isEqualTo(OperationType.RETRACT);
        verify(retractionValidator).validate(
                eq(visitId), eq(organizationId), eq(existing), any());
    }

    @Test
    void shouldRejectInventedTargetFactId() {
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(new PlannerOperation(
                "KEEP", UUID.randomUUID().toString(), null, null)));

        assertThatThrownBy(() -> normalizer.normalize(
                visitId, organizationId, revisionId, List.of(), allowedItems, envelope))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_TARGET_NOT_EFFECTIVE");
    }

    @Test
    void shouldRejectEvidenceOutsideExplicitPlannerBatch() {
        PlannerFact output = new PlannerFact(
                "SYMPTOM",
                "PATIENT_REPORTED",
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "POSITIVE",
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                null,
                null,
                List.of(new PlannerEvidence(
                        UUID.randomUUID().toString(), "douleur abdominale", true)));

        assertThatThrownBy(() -> normalizer.normalize(
                visitId,
                organizationId,
                revisionId,
                List.of(),
                allowedItems,
                new PlannerEnvelope(List.of(add(output)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_EVIDENCE_OUTSIDE_BATCH");
    }

    @Test
    void shouldRejectAmbiguousVerbatimQuote() {
        TranscriptItemView repeated = transcript("douleur puis douleur", "PATIENT");
        allowedItems.clear();
        allowedItems.put(repeated.id(), repeated);
        PlannerFact output = factOutput(
                "PAIN",
                "douleur",
                "douleur",
                repeated,
                Authority.PATIENT_REPORTED,
                null);

        assertThatThrownBy(() -> normalizer.normalize(
                visitId,
                organizationId,
                revisionId,
                List.of(),
                allowedItems,
                new PlannerEnvelope(List.of(add(output)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_QUOTE_AMBIGUOUS");
    }

    @Test
    void shouldRejectPatientEvidenceCreatingClinicalDecision() {
        PlannerFact output = new PlannerFact(
                "ASSESSMENT",
                "PATIENT_REPORTED",
                "MALARIA",
                "douleur abdominale",
                "POSITIVE",
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                null,
                null,
                List.of(new PlannerEvidence(
                        patientItem.id().toString(), "douleur abdominale", true)));

        assertThatThrownBy(() -> normalizer.normalize(
                visitId,
                organizationId,
                revisionId,
                List.of(),
                allowedItems,
                new PlannerEnvelope(List.of(add(output)))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_PATIENT_DECISION_INVALID");
    }

    @Test
    void shouldRejectTwoNormalizedOperationsTargetingSameFact() {
        FactView existing = fact(
                UUID.randomUUID(),
                "ABDOMINAL_PAIN",
                "douleur abdominale",
                "douleur abdominale",
                patientItem.id());
        PlannerEnvelope envelope = new PlannerEnvelope(List.of(
                new PlannerOperation("KEEP", existing.id().toString(), null, null),
                new PlannerOperation("KEEP", existing.id().toString(), null, null)));

        assertThatThrownBy(() -> normalizer.normalize(
                visitId, organizationId, revisionId, List.of(existing), allowedItems, envelope))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_REVISION_PLAN_TARGET_CONFLICT");
    }

    private PlannerOperation add(PlannerFact fact) {
        return new PlannerOperation("ADD", null, fact, null);
    }

    private PlannerFact factOutput(String code, String concept, String quote) {
        return factOutput(code, concept, quote, patientItem, Authority.PATIENT_REPORTED, null);
    }

    private PlannerFact factOutput(
            String code,
            String concept,
            String quote,
            TranscriptItemView item,
            Authority authority,
            String temporality) {
        return new PlannerFact(
                "SYMPTOM",
                authority.name(),
                code,
                concept,
                "POSITIVE",
                null,
                null,
                null,
                temporality,
                "UNSPECIFIED",
                null,
                null,
                List.of(new PlannerEvidence(item.id().toString(), quote, true)));
    }

    private FactView fact(
            UUID id,
            String code,
            String concept,
            String quote,
            UUID transcriptItemId) {
        return fact(id, code, concept, quote, transcriptItemId, Authority.PATIENT_REPORTED);
    }

    private FactView fact(
            UUID id,
            String code,
            String concept,
            String quote,
            UUID transcriptItemId,
            Authority authority) {
        return new FactView(
                id,
                1,
                "existing-" + id,
                FactType.SYMPTOM.name(),
                authority.name(),
                code,
                concept,
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
                        quote.length(),
                        quote,
                        true)));
    }

    private TranscriptItemView transcript(String text, String speakerType) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "planner-test",
                "AMBIENT_DIARIZED",
                speakerType,
                speakerType.toLowerCase(),
                text,
                "fr",
                1_000,
                2_000,
                "FINAL",
                null,
                Instant.now());
    }
}
