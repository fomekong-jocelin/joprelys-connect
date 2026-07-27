package com.joprelys.backend.ai.facts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptLedgerService;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactLedgerView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteSectionCode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClinicalNoteProjectionServiceTest {

    private final ClinicalFactLedgerService factLedgerService = mock(ClinicalFactLedgerService.class);
    private final AmbientTranscriptLedgerService transcriptLedgerService = mock(AmbientTranscriptLedgerService.class);
    private final ClinicalNoteProjectionService service = new ClinicalNoteProjectionService(
            factLedgerService,
            transcriptLedgerService);

    @Test
    void shouldProjectEffectiveFactsIntoStableClinicalSectionsWithLinkedEvidence() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();
        TranscriptItemView transcriptItem = transcript(
                transcriptItemId,
                "PATIENT",
                "patient",
                "J'ai une douleur du genou gauche depuis trois jours.",
                12_000,
                16_000);
        FactView symptom = fact(
                UUID.randomUUID(),
                4,
                "SYMPTOM",
                "PATIENT_REPORTED",
                "KNEE_PAIN",
                "douleur du genou",
                "POSITIVE",
                null,
                null,
                null,
                "depuis trois jours",
                "LEFT",
                null,
                null,
                transcriptItemId,
                9,
                32,
                "douleur du genou gauche");

        when(transcriptLedgerService.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedgerService.listEffectiveForTranscriptSnapshot(
                eq(visitId), eq(organizationId), eq(Set.of(transcriptItemId))))
                .thenReturn(new FactLedgerView(visitId, List.of(symptom)));

        var projection = service.project(visitId, organizationId);

        assertThat(projection.visitId()).isEqualTo(visitId);
        assertThat(projection.projectionVersion())
                .startsWith(ClinicalNoteProjectionService.PROJECTION_SCHEMA_VERSION + ":");
        assertThat(projection.maxFactSequence()).isEqualTo(4);
        assertThat(projection.sections()).hasSize(1);
        assertThat(projection.sections().getFirst().code())
                .isEqualTo(NoteSectionCode.HISTORY_OF_PRESENT_ILLNESS);
        var entry = projection.sections().getFirst().entries().getFirst();
        assertThat(entry.conceptCode()).isEqualTo("KNEE_PAIN");
        assertThat(entry.laterality()).isEqualTo("LEFT");
        assertThat(entry.temporalityText()).isEqualTo("depuis trois jours");
        assertThat(entry.evidence()).hasSize(1);
        assertThat(entry.evidence().getFirst().speakerType()).isEqualTo("PATIENT");
        assertThat(entry.evidence().getFirst().startOffsetMs()).isEqualTo(12_000);
        assertThat(entry.evidence().getFirst().endOffsetMs()).isEqualTo(16_000);
        assertThat(entry.evidence().getFirst().quoteText()).isEqualTo("douleur du genou gauche");
    }

    @Test
    void shouldProduceSameProjectionVersionForSameCanonicalSnapshot() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();
        TranscriptItemView transcriptItem = transcript(
                transcriptItemId,
                "DOCTOR",
                "doctor",
                "Tension artérielle 120 sur 80 mmHg.",
                2_000,
                4_000);
        FactView vital = fact(
                UUID.randomUUID(),
                2,
                "VITAL",
                "CLINICIAN_OBSERVED",
                "BLOOD_PRESSURE",
                "Tension artérielle",
                "POSITIVE",
                "120",
                "80",
                "MMHG",
                null,
                "UNSPECIFIED",
                null,
                null,
                transcriptItemId,
                0,
                34,
                "Tension artérielle 120 sur 80 mmHg");

        when(transcriptLedgerService.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedgerService.listEffectiveForTranscriptSnapshot(
                eq(visitId), eq(organizationId), eq(Set.of(transcriptItemId))))
                .thenReturn(new FactLedgerView(visitId, List.of(vital)));

        var first = service.project(visitId, organizationId);
        var second = service.project(visitId, organizationId);

        assertThat(first.projectionVersion()).isEqualTo(second.projectionVersion());
    }

    @Test
    void shouldFailClosedWhenStoredEvidenceNoLongerMatchesCanonicalTranscriptText() {
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID transcriptItemId = UUID.randomUUID();
        TranscriptItemView transcriptItem = transcript(
                transcriptItemId,
                "PATIENT",
                "patient",
                "Pas de fièvre.",
                0,
                1_000);
        FactView stale = fact(
                UUID.randomUUID(),
                1,
                "SYMPTOM",
                "PATIENT_REPORTED",
                "FEVER",
                "fièvre",
                "NEGATIVE",
                null,
                null,
                null,
                null,
                "UNSPECIFIED",
                null,
                null,
                transcriptItemId,
                7,
                13,
                "toux!!");

        when(transcriptLedgerService.listFinal(visitId, organizationId))
                .thenReturn(new TranscriptLedgerView(visitId, List.of(transcriptItem)));
        when(factLedgerService.listEffectiveForTranscriptSnapshot(
                eq(visitId), eq(organizationId), eq(Set.of(transcriptItemId))))
                .thenReturn(new FactLedgerView(visitId, List.of(stale)));

        assertThatThrownBy(() -> service.project(visitId, organizationId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_CLINICAL_NOTE_EVIDENCE_STALE");
    }

    private TranscriptItemView transcript(
            UUID id,
            String speakerType,
            String speakerLabel,
            String text,
            long startOffsetMs,
            long endOffsetMs) {
        return new TranscriptItemView(
                id,
                1,
                "source-" + id,
                "AMBIENT_DIARIZED",
                speakerType,
                speakerLabel,
                text,
                "fr",
                startOffsetMs,
                endOffsetMs,
                "FINAL",
                null,
                Instant.parse("2026-07-27T12:00:00Z"));
    }

    private FactView fact(
            UUID id,
            long sequence,
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
            UUID transcriptItemId,
            int quoteStart,
            int quoteEnd,
            String quoteText) {
        return new FactView(
                id,
                sequence,
                "event-" + id,
                factType,
                authority,
                conceptCode,
                conceptText,
                polarity,
                valuePrimary,
                valueSecondary,
                unitCode,
                temporalityText,
                laterality,
                frequencyText,
                routeText,
                "ASSERTED",
                null,
                Instant.parse("2026-07-27T12:00:00Z"),
                List.of(new EvidenceSpanView(
                        UUID.randomUUID(),
                        transcriptItemId,
                        quoteStart,
                        quoteEnd,
                        quoteText,
                        true)));
    }
}
