package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.domain.AmbientAudioChunkStatus;
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

class AmbientAudioChunkJournalServiceTest {

    private final AmbientAudioChunkRepository chunkRepository = mock(AmbientAudioChunkRepository.class);
    private final AmbientTranscriptItemRepository transcriptRepository = mock(AmbientTranscriptItemRepository.class);
    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final AmbientTranscriptLedgerService ledgerService = mock(AmbientTranscriptLedgerService.class);
    private final AmbientAudioChunkJournalService service = new AmbientAudioChunkJournalService(
            chunkRepository, transcriptRepository, visitRepository, ledgerService);

    @Test
    void shouldReturnCompletedWithoutReclaimingChunk() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        AmbientAudioChunkEntity chunk = chunk(
                organizationId, visitId, "chunk-1", hash('a'), 10_000, userId, Instant.now());
        chunk.complete(Instant.now());
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-1"))
                .thenReturn(Optional.of(chunk));

        var claim = service.claim(
                visitId, userId, organizationId, "chunk-1", hash('a'), 10_000, "audio/wav");

        assertThat(claim.alreadyCompleted()).isTrue();
        assertThat(chunk.getStatus()).isEqualTo(AmbientAudioChunkStatus.COMPLETED);
    }

    @Test
    void shouldRejectConcurrentRecentProcessingClaim() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        AmbientAudioChunkEntity chunk = chunk(
                organizationId, visitId, "chunk-2", hash('b'), 0, userId, Instant.now());
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-2"))
                .thenReturn(Optional.of(chunk));

        assertThatThrownBy(() -> service.claim(
                visitId, userId, organizationId, "chunk-2", hash('b'), 0, "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CHUNK_PROCESSING");
    }

    @Test
    void shouldReclaimStaleProcessingChunk() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        AmbientAudioChunkEntity chunk = chunk(
                organizationId,
                visitId,
                "chunk-3",
                hash('c'),
                500,
                userId,
                Instant.now().minusSeconds(180));
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-3"))
                .thenReturn(Optional.of(chunk));

        var claim = service.claim(
                visitId, userId, organizationId, "chunk-3", hash('c'), 500, "audio/wav");

        assertThat(claim.alreadyCompleted()).isFalse();
        assertThat(chunk.getStatus()).isEqualTo(AmbientAudioChunkStatus.PROCESSING);
        assertThat(chunk.getClaimedAt()).isAfter(Instant.now().minusSeconds(30));
        verify(chunkRepository).save(chunk);
    }

    @Test
    void shouldRejectSameChunkIdWithDifferentAudioHash() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        AmbientAudioChunkEntity chunk = chunk(
                organizationId, visitId, "chunk-4", hash('d'), 1_000, userId, Instant.now());
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-4"))
                .thenReturn(Optional.of(chunk));

        assertThatThrownBy(() -> service.claim(
                visitId, userId, organizationId, "chunk-4", hash('e'), 1_000, "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CHUNK_HASH_MISMATCH");
    }

    @Test
    void shouldCreateProcessingClaimForNewChunk() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        prepareVisit(visitId, organizationId);
        when(chunkRepository.findByVisitIdAndChunkId(visitId, "chunk-new"))
                .thenReturn(Optional.empty());
        when(chunkRepository.save(any(AmbientAudioChunkEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var claim = service.claim(
                visitId, userId, organizationId, "chunk-new", hash('f'), 0, "audio/wav");

        assertThat(claim.alreadyCompleted()).isFalse();
        verify(chunkRepository).save(any(AmbientAudioChunkEntity.class));
    }

    private void prepareVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = mock(VisitEntity.class);
        when(visit.getOrganizationId()).thenReturn(organizationId);
        when(visitRepository.findByIdForUpdate(visitId)).thenReturn(Optional.of(visit));
    }

    private AmbientAudioChunkEntity chunk(
            UUID organizationId,
            UUID visitId,
            String chunkId,
            String hash,
            long offset,
            UUID userId,
            Instant claimedAt) {
        return new AmbientAudioChunkEntity(
                organizationId,
                visitId,
                chunkId,
                hash,
                offset,
                "audio/wav",
                userId,
                claimedAt);
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
