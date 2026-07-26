package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
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

class ClinicalFactEvidenceValidatorTest {

    private final AmbientTranscriptLedgerService transcriptLedger = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalFactEvidenceValidator validator = new ClinicalFactEvidenceValidator(transcriptLedger);

    @Test
    void shouldKeepBloodPressureAndHeartRateBoundToTheirOwnClauses() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item(
                "DOCTOR",
                "Tension 120/80, pouls 72.");
        effective(visitId, organizationId, item);

        FactCandidate bloodPressure = candidate(
                "bp-1", FactType.VITAL, Authority.CLINICIAN_OBSERVED,
                "BLOOD_PRESSURE", "Tension", Polarity.POSITIVE,
                "120", "80", "MMHG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, "Tension 120/80"));
        FactCandidate heartRate = candidate(
                "hr-1", FactType.VITAL, Authority.CLINICIAN_OBSERVED,
                "HEART_RATE", "pouls", Polarity.POSITIVE,
                "72", null, "BPM", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, "pouls 72"));

        assertThat(validator.validate(visitId, organizationId, bloodPressure)).isNotNull();
        assertThat(validator.validate(visitId, organizationId, heartRate)).isNotNull();

        FactCandidate swapped = candidate(
                "bp-wrong", FactType.VITAL, Authority.CLINICIAN_OBSERVED,
                "BLOOD_PRESSURE", "Tension", Polarity.POSITIVE,
                "72", "80", "MMHG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, "Tension 120/80, pouls 72"));
        assertThatThrownBy(() -> validator.validate(visitId, organizationId, swapped))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_RELATION_AMBIGUOUS");
    }

    @Test
    void shouldRejectMedicationDoseTakenFromAnotherDrugInSameClause() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item(
                "DOCTOR",
                "Paracétamol 500 mg et ibuprofène 200 mg");
        effective(visitId, organizationId, item);

        FactCandidate correct = candidate(
                "med-ok", FactType.MEDICATION, Authority.CLINICIAN_DECISION,
                "PARACETAMOL", "Paracétamol", Polarity.POSITIVE,
                "500", null, "MG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));
        assertThat(validator.validate(visitId, organizationId, correct)).isNotNull();

        FactCandidate swapped = candidate(
                "med-wrong", FactType.MEDICATION, Authority.CLINICIAN_DECISION,
                "PARACETAMOL", "Paracétamol", Polarity.POSITIVE,
                "200", null, "MG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));
        assertThatThrownBy(() -> validator.validate(visitId, organizationId, swapped))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_MEDICATION_RELATION_MISMATCH");
    }

    @Test
    void shouldRejectNegationFlipForSymptom() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item(
                "PATIENT",
                "Sans douleur thoracique depuis 2 jours");
        effective(visitId, organizationId, item);

        FactCandidate negative = candidate(
                "sym-neg", FactType.SYMPTOM, Authority.PATIENT_REPORTED,
                "CHEST_PAIN", "douleur thoracique", Polarity.NEGATIVE,
                null, null, null, "depuis 2 jours", Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));
        assertThat(validator.validate(visitId, organizationId, negative)).isNotNull();

        FactCandidate positive = candidate(
                "sym-pos", FactType.SYMPTOM, Authority.PATIENT_REPORTED,
                "CHEST_PAIN", "douleur thoracique", Polarity.POSITIVE,
                null, null, null, "depuis 2 jours", Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));
        assertThatThrownBy(() -> validator.validate(visitId, organizationId, positive))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_NEGATION_RELATION_MISMATCH");
    }

    @Test
    void shouldRejectWrongLaterality() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("PATIENT", "Douleur du genou gauche");
        effective(visitId, organizationId, item);

        FactCandidate wrongSide = candidate(
                "side-wrong", FactType.SYMPTOM, Authority.PATIENT_REPORTED,
                "KNEE_PAIN", "Douleur du genou", Polarity.POSITIVE,
                null, null, null, null, Laterality.RIGHT,
                null, null,
                evidence(item, item.text()));

        assertThatThrownBy(() -> validator.validate(visitId, organizationId, wrongSide))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_LATERALITY_NOT_IN_EVIDENCE");
    }

    @Test
    void shouldRequireDoctorEvidenceForClinicianDecision() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("PATIENT", "Je prends paracétamol 500 mg");
        effective(visitId, organizationId, item);

        FactCandidate decision = candidate(
                "decision-1", FactType.MEDICATION, Authority.CLINICIAN_DECISION,
                "PARACETAMOL", "paracétamol", Polarity.POSITIVE,
                "500", null, "MG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));

        assertThatThrownBy(() -> validator.validate(visitId, organizationId, decision))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_DOCTOR_EVIDENCE_REQUIRED");
    }

    @Test
    void shouldRejectEvidenceThatIsNoLongerInEffectiveTranscript() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView superseded = item("DOCTOR", "Tension 120/80");
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of()));

        FactCandidate fact = candidate(
                "stale-1", FactType.VITAL, Authority.CLINICIAN_OBSERVED,
                "BLOOD_PRESSURE", "Tension", Polarity.POSITIVE,
                "120", "80", "MMHG", null, Laterality.UNSPECIFIED,
                null, null,
                evidence(superseded, superseded.text()));

        assertThatThrownBy(() -> validator.validate(visitId, organizationId, fact))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_EVIDENCE_NOT_EFFECTIVE");
    }

    @Test
    void shouldRejectUnspecifiedSpeakerAsOnlyPatientReportedEvidence() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        TranscriptItemView item = item("UNSPECIFIED", "Douleur abdominale");
        effective(visitId, organizationId, item);

        FactCandidate fact = candidate(
                "speaker-1", FactType.SYMPTOM, Authority.PATIENT_REPORTED,
                "ABDOMINAL_PAIN", "Douleur abdominale", Polarity.POSITIVE,
                null, null, null, null, Laterality.UNSPECIFIED,
                null, null,
                evidence(item, item.text()));

        assertThatThrownBy(() -> validator.validate(visitId, organizationId, fact))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_FACT_EXPLICIT_SPEAKER_REQUIRED");
    }

    private void effective(UUID visitId, UUID organizationId, TranscriptItemView... items) {
        when(transcriptLedger.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(items)));
    }

    private EvidenceSpanCandidate evidence(TranscriptItemView item, String quote) {
        int start = item.text().indexOf(quote);
        return new EvidenceSpanCandidate(
                item.id(),
                start,
                start + quote.length(),
                quote,
                true);
    }

    private FactCandidate candidate(
            String eventId,
            FactType type,
            Authority authority,
            String conceptCode,
            String conceptText,
            Polarity polarity,
            String primary,
            String secondary,
            String unit,
            String temporality,
            Laterality laterality,
            String frequency,
            String route,
            EvidenceSpanCandidate evidence) {
        return new FactCandidate(
                eventId,
                type,
                authority,
                conceptCode,
                conceptText,
                polarity,
                primary,
                secondary,
                unit,
                temporality,
                laterality,
                frequency,
                route,
                FactStatus.ASSERTED,
                null,
                List.of(evidence));
    }

    private TranscriptItemView item(String speaker, String text) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "source-1",
                "AMBIENT_DIARIZED",
                speaker,
                speaker,
                text,
                "fr",
                0,
                1_000,
                "FINAL",
                null,
                Instant.now());
    }
}
