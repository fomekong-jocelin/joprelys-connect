package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiTranscriptionWorkflowTest {

    @Test
    void shouldKeepLowConfidenceTranscriptPendingForHumanReview() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription(
                        "Patient présentant une douleur abdominale depuis trois jours.",
                        "fr",
                        0.08));

        AiTranscriptionWorkflow workflow = new AiTranscriptionWorkflow(provider, properties());
        AiConsultationSessionState state = state();

        var result = workflow.transcribe(state, new byte[] {1, 2, 3}, "audio/webm");

        assertEquals(
                "Patient présentant une douleur abdominale depuis trois jours.",
                result.transcript());
        assertEquals("PENDING_REVIEW", result.status());
        assertEquals(result.transcript(), state.pendingTranscript);
        assertEquals("PENDING_REVIEW", state.transcriptStatus);
        verify(provider).transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr"));
    }

    @Test
    void shouldStillRejectBlankProviderTranscript() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription("   ", "fr", 0.99));

        AiTranscriptionWorkflow workflow = new AiTranscriptionWorkflow(provider, properties());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> workflow.transcribe(state(), new byte[] {1, 2, 3}, "audio/webm"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("AI_OUTPUT_INVALID", exception.getReason());
    }

    private AiProperties properties() {
        return new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                0.35,
                "fr",
                null,
                null,
                null);
    }

    private AiConsultationSessionState state() {
        return new AiConsultationSessionState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(1800),
                "fr");
    }
}
