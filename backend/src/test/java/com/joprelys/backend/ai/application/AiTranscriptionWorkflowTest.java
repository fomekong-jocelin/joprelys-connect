package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiTranscriptionWorkflowTest {

    @Test
    void shouldKeepLowConfidenceTranscriptPendingForHumanReview() {
        AiProvider provider = mock(AiProvider.class);
        when(provider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription(
                        "Patient présentant une douleur abdominale depuis trois jours.",
                        "fr",
                        0.08));

        AiProperties properties = new AiProperties(
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
        AiTranscriptionWorkflow workflow = new AiTranscriptionWorkflow(provider, properties);
        AiConsultationSessionState state = new AiConsultationSessionState(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now().plusSeconds(1800),
                "fr");

        var result = workflow.transcribe(state, new byte[] {1, 2, 3}, "audio/webm");

        assertEquals(
                "Patient présentant une douleur abdominale depuis trois jours.",
                result.transcript());
        assertEquals("PENDING_REVIEW", result.status());
        assertEquals(result.transcript(), state.pendingTranscript);
        assertEquals("PENDING_REVIEW", state.transcriptStatus);
    }
}
