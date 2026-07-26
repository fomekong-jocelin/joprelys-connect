package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedSegment;
import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedTranscript;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class AmbientTranscriptionServiceTest {

    private final AmbientDiarizationPort diarizationPort = mock(AmbientDiarizationPort.class);
    private final AmbientAudioChunkJournalService chunkJournal = mock(AmbientAudioChunkJournalService.class);
    private final AmbientTranscriptionService service =
            new AmbientTranscriptionService(diarizationPort, chunkJournal);

    @Test
    void shouldClaimHashBeforeCallingProviderAndCompleteOnce() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        byte[] audio = "stable-audio".getBytes(StandardCharsets.UTF_8);
        var segment = new DiarizedSegment("seg-1", 0, 1.2, "Bonjour", "A");
        var expected = List.of(view(visitId));

        when(chunkJournal.claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-1"),
                org.mockito.ArgumentMatchers.anyString(), eq(12_000L), eq("audio/wav")))
                .thenReturn(new AmbientAudioChunkJournalService.ChunkClaim(false));
        when(diarizationPort.transcribe(audio, "audio/wav", "fr"))
                .thenReturn(new DiarizedTranscript("Bonjour", List.of(segment)));
        when(chunkJournal.complete(
                visitId, userId, organizationId, "chunk-1", 12_000L, "fr", List.of(segment)))
                .thenReturn(expected);

        var result = service.ingestAudioChunk(
                visitId, userId, organizationId, "chunk-1", 12_000L, "fr", audio, "audio/wav");

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(chunkJournal).claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-1"),
                hash.capture(), eq(12_000L), eq("audio/wav"));
        assertThat(hash.getValue()).matches("[0-9a-f]{64}");
        verify(diarizationPort).transcribe(audio, "audio/wav", "fr");
    }

    @Test
    void shouldNeverCallProviderAgainWhenServerAlreadyCompletedChunk() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        byte[] audio = "same-audio".getBytes(StandardCharsets.UTF_8);
        var expected = List.of(view(visitId));

        when(chunkJournal.claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-retry"),
                org.mockito.ArgumentMatchers.anyString(), eq(0L), eq("audio/wav")))
                .thenReturn(new AmbientAudioChunkJournalService.ChunkClaim(true));
        when(chunkJournal.completedItems(visitId, "chunk-retry")).thenReturn(expected);

        var result = service.ingestAudioChunk(
                visitId, userId, organizationId, "chunk-retry", 0, "fr", audio, "audio/wav");

        assertThat(result).isEqualTo(expected);
        verify(diarizationPort, never()).transcribe(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(chunkJournal, never()).complete(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-retry"),
                eq(0L), eq("fr"), anyList());
    }

    @Test
    void shouldKeepChunkRetryableWhenProviderFails() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        byte[] audio = "audio".getBytes(StandardCharsets.UTF_8);

        when(chunkJournal.claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-fail"),
                org.mockito.ArgumentMatchers.anyString(), eq(100L), eq("audio/wav")))
                .thenReturn(new AmbientAudioChunkJournalService.ChunkClaim(false));
        when(diarizationPort.transcribe(audio, "audio/wav", "fr"))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "AI_AMBIENT_UPSTREAM_UNAVAILABLE"));

        assertThatThrownBy(() -> service.ingestAudioChunk(
                visitId, userId, organizationId, "chunk-fail", 100, "fr", audio, "audio/wav"))
                .isInstanceOf(ResponseStatusException.class);

        verify(chunkJournal).fail(
                visitId, organizationId, "chunk-fail", "AI_AMBIENT_UPSTREAM_UNAVAILABLE");
    }

    @Test
    void shouldRejectInvalidChunkIdBeforeProviderOrJournal() {
        assertThatThrownBy(() -> service.ingestAudioChunk(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "bad chunk id", 0, "fr", new byte[]{1}, "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_CHUNK_ID_INVALID");

        verify(diarizationPort, never()).transcribe(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private TranscriptItemView view(UUID visitId) {
        return new TranscriptItemView(
                UUID.randomUUID(),
                1,
                "chunk-1:seg-1",
                "AMBIENT_DIARIZED",
                "UNSPECIFIED",
                "A",
                "Bonjour",
                "fr",
                0,
                1_000,
                "FINAL",
                null,
                java.time.Instant.now());
    }
}
