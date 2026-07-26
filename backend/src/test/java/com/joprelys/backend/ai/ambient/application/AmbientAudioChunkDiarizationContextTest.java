package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkRepository;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AmbientAudioChunkDiarizationContextTest {

    private final AmbientAudioChunkRepository chunkRepository = mock(AmbientAudioChunkRepository.class);
    private final AmbientTranscriptItemRepository transcriptRepository = mock(AmbientTranscriptItemRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AmbientTranscriptLedgerService ledgerService = mock(AmbientTranscriptLedgerService.class);
    private final AmbientAudioChunkJournalService service = new AmbientAudioChunkJournalService(
            chunkRepository, transcriptRepository, visitRepository, ledgerService);

    @Test
    void shouldPersistDiarizationContextHashForNewChunk() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        String contextHash = "a".repeat(64);
        prepareVisit(visitId, organizationId);
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-context"))
                .thenReturn(Optional.empty());
        when(chunkRepository.save(any(AmbientAudioChunkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var claim = service.claim(
                visitId,
                userId,
                organizationId,
                "chunk-context",
                "b".repeat(64),
                contextHash,
                0,
                "audio/wav");

        assertThat(claim.alreadyCompleted()).isFalse();
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(AmbientAudioChunkEntity.class);
        org.mockito.Mockito.verify(chunkRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getDiarizationContextSha256()).isEqualTo(contextHash);
    }

    @Test
    void shouldRejectSameChunkWhenKnownSpeakerContextChanges() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        AmbientAudioChunkEntity existing = new AmbientAudioChunkEntity(
                organizationId,
                visitId,
                "chunk-context-mismatch",
                "c".repeat(64),
                "d".repeat(64),
                1_000,
                "audio/wav",
                userId,
                Instant.now());
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-context-mismatch"))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.claim(
                visitId,
                userId,
                organizationId,
                "chunk-context-mismatch",
                "c".repeat(64),
                "e".repeat(64),
                1_000,
                "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CHUNK_DIARIZATION_CONTEXT_MISMATCH");
    }

    @Test
    void legacyClaimShouldUseEmptyDiarizationContext() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "legacy"))
                .thenReturn(Optional.empty());
        when(chunkRepository.save(any(AmbientAudioChunkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.claim(
                visitId,
                userId,
                organizationId,
                "legacy",
                "f".repeat(64),
                0,
                "audio/wav");

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(AmbientAudioChunkEntity.class);
        org.mockito.Mockito.verify(chunkRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getDiarizationContextSha256())
                .isEqualTo(AmbientAudioChunkEntity.EMPTY_DIARIZATION_CONTEXT_SHA256);
    }

    private void prepareVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }
}
