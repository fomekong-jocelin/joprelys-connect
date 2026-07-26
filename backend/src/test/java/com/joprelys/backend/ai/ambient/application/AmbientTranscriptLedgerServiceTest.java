package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
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
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
        when(transcriptRepository.findMaximumSequence(visitId)).thenReturn(9L);

        AmbientTranscriptItemEntity existing = new AmbientTranscriptItemEntity(
                organizationId,
                visitId,
                7,
                "chunk-002:seg-1",
                AmbientTranscriptSource.AMBIENT_DIARIZED,
                AmbientTranscriptSpeaker.PATIENT,
                "patient",
                "Texte déjà persisté",
                "fr",
                20_000,
                21_000,
                AmbientTranscriptStatus.FINAL,
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
}
