package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedSegment;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSource;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSpeaker;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptStatus;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AmbientTranscriptLedgerServiceTest {

    private final AmbientTranscriptItemRepository transcriptRepository = mock(AmbientTranscriptItemRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AmbientTranscriptLedgerService service =
            new AmbientTranscriptLedgerService(transcriptRepository, visitRepository);

    @Test
    void shouldPersistFinalSegmentsInChronologicalOrderAndKeepUnknownSpeakerSafe() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findMaximumSequence(visitId)).thenReturn(4L);
        when(transcriptRepository.findByVisitIdAndSourceEventId(any(), any())).thenReturn(Optional.empty());
        when(transcriptRepository.save(any(AmbientTranscriptItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AmbientTranscriptContract.TranscriptItemView> saved = service.appendDiarizedSegments(
                visitId,
                userId,
                organizationId,
                "chunk-001",
                10_000,
                "fr",
                List.of(
                        new DiarizedSegment("seg-b", 3.0, 4.0, "Deuxième segment", "B"),
                        new DiarizedSegment("seg-a", 1.0, 2.0, "Premier segment", "doctor")));

        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).text()).isEqualTo("Premier segment");
        assertThat(saved.get(0).speakerType()).isEqualTo("DOCTOR");
        assertThat(saved.get(0).startOffsetMs()).isEqualTo(11_000);
        assertThat(saved.get(0).sequence()).isEqualTo(5);
        assertThat(saved.get(1).speakerType()).isEqualTo("UNSPECIFIED");
        assertThat(saved.get(1).speakerLabel()).isEqualTo("B");
        assertThat(saved.get(1).sequence()).isEqualTo(6);
        verify(visitRepository).findByIdForUpdate(visitId);
    }

    @Test
    void shouldReturnExistingItemWhenChunkIsRetried() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findMaximumSequence(visitId)).thenReturn(9L);

        AmbientTranscriptItemEntity existing = item(
                organizationId,
                visitId,
                7,
                "chunk-002:seg-1",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.PATIENT,
                "patient",
                "Texte déjà persisté",
                20_000,
                21_000,
                userId,
                null);
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "chunk-002:seg-1"))
                .thenReturn(Optional.of(existing));

        List<AmbientTranscriptContract.TranscriptItemView> result = service.appendDiarizedSegments(
                visitId,
                userId,
                organizationId,
                "chunk-002",
                20_000,
                "fr",
                List.of(new DiarizedSegment("seg-1", 0, 1, "Texte déjà persisté", "patient")));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().sequence()).isEqualTo(7);
        assertThat(result.getFirst().sourceEventId()).isEqualTo("chunk-002:seg-1");
    }

    @Test
    void shouldAppendHumanSpeakerCorrectionWithoutMutatingOriginal() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity original = item(
                organizationId,
                visitId,
                3,
                "chunk-003:seg-1",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED,
                "A",
                "Je prescris le traitement indiqué.",
                30_000,
                31_500,
                userId,
                null);

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "correction:corr-1"))
                .thenReturn(Optional.empty());
        when(transcriptRepository.findByIdAndVisitId(original.getId(), visitId))
                .thenReturn(Optional.of(original));
        when(transcriptRepository.findByVisitIdAndSupersedesItemId(visitId, original.getId()))
                .thenReturn(Optional.empty());
        when(transcriptRepository.findMaximumSequence(visitId)).thenReturn(3L);
        when(transcriptRepository.saveAndFlush(any(AmbientTranscriptItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var correction = service.appendCorrection(
                visitId,
                original.getId(),
                userId,
                organizationId,
                "corr-1",
                "DOCTOR",
                "Je prescris le traitement indiqué.");

        assertThat(correction.sequence()).isEqualTo(4);
        assertThat(correction.source()).isEqualTo("MANUAL_CORRECTION");
        assertThat(correction.speakerType()).isEqualTo("DOCTOR");
        assertThat(correction.supersedesItemId()).isEqualTo(original.getId());
        assertThat(original.getSpeakerType()).isEqualTo(AmbientTranscriptSpeaker.UNSPECIFIED);
    }

    @Test
    void shouldRejectCorrectionOfAlreadySupersededVersion() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity original = item(
                organizationId, visitId, 1, "chunk:1",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED, "A", "Texte initial",
                0, 1_000, userId, null);
        AmbientTranscriptItemEntity firstCorrection = item(
                organizationId, visitId, 2, "correction:first",
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.PATIENT, "human:PATIENT", "Texte corrigé",
                0, 1_000, userId, original.getId());

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "correction:second"))
                .thenReturn(Optional.empty());
        when(transcriptRepository.findByIdAndVisitId(original.getId(), visitId))
                .thenReturn(Optional.of(original));
        when(transcriptRepository.findByVisitIdAndSupersedesItemId(visitId, original.getId()))
                .thenReturn(Optional.of(firstCorrection));

        assertThatThrownBy(() -> service.appendCorrection(
                visitId, original.getId(), userId, organizationId,
                "second", "PATIENT", "Autre texte"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_TRANSCRIPT_ITEM_SUPERSEDED");

        verify(transcriptRepository, never()).saveAndFlush(any(AmbientTranscriptItemEntity.class));
    }

    @Test
    void shouldAllowLinearCorrectionChainAndExposeOnlyLeaf() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity original = item(
                organizationId, visitId, 1, "chunk:root",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED, "A", "Version zéro",
                0, 1_000, userId, null);
        AmbientTranscriptItemEntity correction1 = item(
                organizationId, visitId, 2, "correction:c1",
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.PATIENT, "human:PATIENT", "Version un",
                0, 1_000, userId, original.getId());

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "correction:c2"))
                .thenReturn(Optional.empty());
        when(transcriptRepository.findByIdAndVisitId(correction1.getId(), visitId))
                .thenReturn(Optional.of(correction1));
        when(transcriptRepository.findByVisitIdAndSupersedesItemId(visitId, correction1.getId()))
                .thenReturn(Optional.empty());
        when(transcriptRepository.findMaximumSequence(visitId)).thenReturn(2L);
        when(transcriptRepository.saveAndFlush(any(AmbientTranscriptItemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var correction2 = service.appendCorrection(
                visitId, correction1.getId(), userId, organizationId,
                "c2", "DOCTOR", "Version deux");

        AmbientTranscriptItemEntity correction2Entity = item(
                organizationId, visitId, correction2.sequence(), correction2.sourceEventId(),
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.DOCTOR, "human:DOCTOR", correction2.text(),
                correction2.startOffsetMs(), correction2.endOffsetMs(), userId, correction1.getId());
        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndStatusOrderByStartOffsetMsAscSequenceNoAsc(
                visitId, AmbientTranscriptStatus.FINAL))
                .thenReturn(List.of(original, correction1, correction2Entity));

        var effective = service.listFinal(visitId, organizationId);

        assertThat(effective.items()).hasSize(1);
        assertThat(effective.items().getFirst().text()).isEqualTo("Version deux");
        assertThat(effective.items().getFirst().speakerType()).isEqualTo("DOCTOR");
    }

    @Test
    void shouldReturnSameCorrectionForStrictlyIdenticalRetry() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity parent = item(
                organizationId, visitId, 1, "chunk:retry",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED, "A", "Texte initial",
                0, 1_000, userId, null);
        AmbientTranscriptItemEntity existing = item(
                organizationId, visitId, 2, "correction:retry-1",
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.DOCTOR, "human:DOCTOR", "Texte confirmé",
                0, 1_000, userId, parent.getId());

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "correction:retry-1"))
                .thenReturn(Optional.of(existing));

        var retry = service.appendCorrection(
                visitId, parent.getId(), userId, organizationId,
                "retry-1", "DOCTOR", "Texte confirmé");

        assertThat(retry.sequence()).isEqualTo(2);
        assertThat(retry.text()).isEqualTo("Texte confirmé");
        verify(transcriptRepository, never()).saveAndFlush(any(AmbientTranscriptItemEntity.class));
    }

    @Test
    void shouldRejectCorrectionIdReuseWithDifferentParentSpeakerOrText() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity parent = item(
                organizationId, visitId, 1, "chunk:parent",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED, "A", "Texte initial",
                0, 1_000, userId, null);
        AmbientTranscriptItemEntity existing = item(
                organizationId, visitId, 2, "correction:same-id",
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.DOCTOR, "human:DOCTOR", "Texte confirmé",
                0, 1_000, userId, parent.getId());

        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndSourceEventId(visitId, "correction:same-id"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.appendCorrection(
                visitId, UUID.randomUUID(), userId, organizationId,
                "same-id", "DOCTOR", "Texte confirmé"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CORRECTION_ID_REUSED");

        assertThatThrownBy(() -> service.appendCorrection(
                visitId, parent.getId(), userId, organizationId,
                "same-id", "PATIENT", "Texte confirmé"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CORRECTION_ID_REUSED");

        assertThatThrownBy(() -> service.appendCorrection(
                visitId, parent.getId(), userId, organizationId,
                "same-id", "DOCTOR", "Texte différent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CORRECTION_ID_REUSED");
    }

    @Test
    void shouldExposeOnlyLatestEffectiveVersionButKeepAuditTrail() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        VisitEntity visit = visit(visitId, organizationId);
        AmbientTranscriptItemEntity original = item(
                organizationId,
                visitId,
                1,
                "chunk-004:seg-1",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.UNSPECIFIED,
                "A",
                "Texte initial",
                0,
                1_000,
                userId,
                null);
        AmbientTranscriptItemEntity correction = item(
                organizationId,
                visitId,
                2,
                "correction:corr-2",
                AmbientTranscriptSource.MANUAL_CORRECTION,
                AmbientTranscriptSpeaker.PATIENT,
                "human:PATIENT",
                "Texte corrigé",
                0,
                1_000,
                userId,
                original.getId());

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findByVisitIdAndStatusOrderByStartOffsetMsAscSequenceNoAsc(
                visitId, AmbientTranscriptStatus.FINAL))
                .thenReturn(List.of(original, correction));
        when(transcriptRepository.findByVisitIdOrderByStartOffsetMsAscSequenceNoAsc(visitId))
                .thenReturn(List.of(original, correction));

        var effective = service.listFinal(visitId, organizationId);
        var audit = service.listAudit(visitId, organizationId);

        assertThat(effective.items()).hasSize(1);
        assertThat(effective.items().getFirst().text()).isEqualTo("Texte corrigé");
        assertThat(effective.items().getFirst().speakerType()).isEqualTo("PATIENT");
        assertThat(audit.items()).hasSize(2);
    }

    private VisitEntity visit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getId()).thenReturn(visitId);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        return visit;
    }

    private AmbientTranscriptItemEntity item(
            UUID organizationId,
            UUID visitId,
            long sequence,
            String sourceEventId,
            AmbientTranscriptSource source,
            AmbientTranscriptSpeaker speaker,
            String speakerLabel,
            String text,
            long startOffset,
            long endOffset,
            UUID userId,
            UUID supersedes) {
        return new AmbientTranscriptItemEntity(
                organizationId,
                visitId,
                sequence,
                sourceEventId,
                source,
                speaker,
                speakerLabel,
                text,
                "fr",
                startOffset,
                endOffset,
                AmbientTranscriptStatus.FINAL,
                userId,
                supersedes);
    }
}
