package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedSegment;
import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedTranscript;
import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.KnownSpeakerReference;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AmbientKnownSpeakerTranscriptionServiceTest {

    private final AmbientDiarizationPort diarizationPort = mock(AmbientDiarizationPort.class);
    private final AmbientAudioChunkJournalService chunkJournal = mock(AmbientAudioChunkJournalService.class);
    private final AmbientTranscriptionService service =
            new AmbientTranscriptionService(diarizationPort, chunkJournal);

    @Test
    void shouldFingerprintKnownDoctorContextBeforeCallingProvider() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        UUID claimToken = UUID.randomUUID();
        byte[] ambientAudio = new byte[]{1, 2, 3};
        var doctor = new KnownSpeakerReference("doctor", new byte[]{7, 8, 9}, "audio/wav");
        var segment = new DiarizedSegment("seg-1", 0, 1, "Bonjour", "doctor");

        when(chunkJournal.claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-known"),
                anyString(), anyString(), eq(1_000L), eq("audio/wav")))
                .thenReturn(new AmbientAudioChunkJournalService.ChunkClaim(false, claimToken, 1));
        when(diarizationPort.transcribe(
                eq(ambientAudio), eq("audio/wav"), eq("fr"), anyList()))
                .thenReturn(new DiarizedTranscript("Bonjour", List.of(segment)));
        when(chunkJournal.complete(
                visitId, userId, organizationId, "chunk-known", claimToken,
                1_000L, "fr", List.of(segment)))
                .thenReturn(List.of());

        service.ingestAudioChunk(
                visitId,
                userId,
                organizationId,
                "chunk-known",
                1_000,
                "fr",
                ambientAudio,
                "audio/wav",
                List.of(doctor));

        ArgumentCaptor<String> contextHash = ArgumentCaptor.forClass(String.class);
        verify(chunkJournal).claim(
                eq(visitId), eq(userId), eq(organizationId), eq("chunk-known"),
                anyString(), contextHash.capture(), eq(1_000L), eq("audio/wav"));
        assertThat(contextHash.getValue()).matches("[0-9a-f]{64}");
        assertThat(contextHash.getValue()).isNotEqualTo(
                com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkEntity
                        .EMPTY_DIARIZATION_CONTEXT_SHA256);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<KnownSpeakerReference>> references = ArgumentCaptor.forClass(List.class);
        verify(diarizationPort).transcribe(
                eq(ambientAudio), eq("audio/wav"), eq("fr"), references.capture());
        assertThat(references.getValue()).hasSize(1);
        assertThat(references.getValue().getFirst().name()).isEqualTo("doctor");
        assertThat(references.getValue().getFirst().audio()).containsExactly(7, 8, 9);
    }

    @Test
    void shouldProduceDifferentContextFingerprintWhenReferenceAudioChanges() {
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        byte[] ambientAudio = new byte[]{4, 5, 6};

        when(chunkJournal.claim(
                eq(visitId), eq(userId), eq(organizationId), anyString(),
                anyString(), anyString(), eq(0L), eq("audio/wav")))
                .thenReturn(new AmbientAudioChunkJournalService.ChunkClaim(true, UUID.randomUUID(), 1));
        when(chunkJournal.completedItems(eq(visitId), anyString())).thenReturn(List.of());

        service.ingestAudioChunk(
                visitId, userId, organizationId, "chunk-a", 0, "fr", ambientAudio, "audio/wav",
                List.of(new KnownSpeakerReference("doctor", new byte[]{1, 1}, "audio/wav")));
        service.ingestAudioChunk(
                visitId, userId, organizationId, "chunk-b", 0, "fr", ambientAudio, "audio/wav",
                List.of(new KnownSpeakerReference("doctor", new byte[]{2, 2}, "audio/wav")));

        ArgumentCaptor<String> hashes = ArgumentCaptor.forClass(String.class);
        verify(chunkJournal, org.mockito.Mockito.times(2)).claim(
                eq(visitId), eq(userId), eq(organizationId), anyString(),
                anyString(), hashes.capture(), eq(0L), eq("audio/wav"));
        assertThat(hashes.getAllValues()).hasSize(2);
        assertThat(hashes.getAllValues().get(0)).isNotEqualTo(hashes.getAllValues().get(1));
    }
}
