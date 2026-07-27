package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClinicalFactRetractionValidatorTest {

    private final AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalFactRetractionValidator validator = new ClinicalFactRetractionValidator(transcriptLedger);

    @Test
    void shouldAcceptClinicianCancellationOnlyFromExactFinalDoctorEvidence() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String text = "J'annule amoxicilline immédiatement";
        TranscriptItemView item = finalItem(text, "DOCTOR");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(item)));
        EvidenceSpanCandidate evidence = span(item, text);
        RetractionPayload payload = new RetractionPayload(
                Authority.CLINICIAN_DECISION,
                RetractionReason.CLINICIAN_CANCELLATION,
                List.of(evidence));

        var result = validator.validate(
                visitId,
                organizationId,
                fact(FactType.MEDICATION, "amoxicilline"),
                payload);

        assertThat(result).containsExactly(evidence);
    }

    @Test
    void shouldRejectClinicianCancellationWhenEvidenceIsNotDoctorSpeech() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String text = "J'annule amoxicilline immédiatement";
        TranscriptItemView item = finalItem(text, "PATIENT");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(item)));
        RetractionPayload payload = new RetractionPayload(
                Authority.CLINICIAN_DECISION,
                RetractionReason.CLINICIAN_CANCELLATION,
                List.of(span(item, text)));

        assertThatThrownBy(() -> validator.validate(
                visitId,
                organizationId,
                fact(FactType.MEDICATION, "amoxicilline"),
                payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_RETRACTION_DOCTOR_EVIDENCE_REQUIRED");
    }

    @Test
    void shouldAcceptExplicitPatientNegationThatNamesTheRetractedConcept() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String text = "Je n'ai pas de fièvre";
        TranscriptItemView item = finalItem(text, "PATIENT");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(item)));
        RetractionPayload payload = new RetractionPayload(
                Authority.PATIENT_REPORTED,
                RetractionReason.EXPLICIT_NEGATION,
                List.of(span(item, text)));

        var result = validator.validate(
                visitId,
                organizationId,
                fact(FactType.SYMPTOM, "fièvre"),
                payload);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldRejectRetractionWhenFinalEvidenceDoesNotNameTargetConcept() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String text = "Correction, je n'ai pas de nausée";
        TranscriptItemView item = finalItem(text, "PATIENT");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(item)));
        RetractionPayload payload = new RetractionPayload(
                Authority.PATIENT_REPORTED,
                RetractionReason.EXPLICIT_CORRECTION,
                List.of(span(item, text)));

        assertThatThrownBy(() -> validator.validate(
                visitId,
                organizationId,
                fact(FactType.SYMPTOM, "fièvre"),
                payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_RETRACTION_CONCEPT_NOT_IN_EVIDENCE");
    }

    @Test
    void shouldRejectRetractionWhoseEvidenceIsNoLongerFinalAndEffective() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView stale = finalItem("Je n'ai pas de fièvre", "PATIENT");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of()));
        RetractionPayload payload = new RetractionPayload(
                Authority.PATIENT_REPORTED,
                RetractionReason.EXPLICIT_NEGATION,
                List.of(span(stale, stale.text())));

        assertThatThrownBy(() -> validator.validate(
                visitId,
                organizationId,
                fact(FactType.SYMPTOM, "fièvre"),
                payload))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_RETRACTION_EVIDENCE_NOT_EFFECTIVE");
    }

    private TranscriptItemView finalItem(String text, String speakerType) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "revision-evidence",
                "AMBIENT_DIARIZED",
                speakerType,
                speakerType.toLowerCase(),
                text,
                "fr",
                5_000,
                8_000,
                "FINAL",
                null,
                Instant.now());
    }

    private EvidenceSpanCandidate span(TranscriptItemView item, String quote) {
        return new EvidenceSpanCandidate(
                item.id(),
                0,
                quote.length(),
                quote,
                true);
    }

    private FactView fact(FactType type, String concept) {
        return new FactView(
                UUID.randomUUID(),
                1,
                "fact-source",
                type.name(),
                Authority.PATIENT_REPORTED.name(),
                "TEST_CONCEPT",
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
                List.of());
    }
}
