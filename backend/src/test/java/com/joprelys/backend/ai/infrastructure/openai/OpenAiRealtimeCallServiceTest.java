package com.joprelys.backend.ai.infrastructure.openai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OpenAiRealtimeCallServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldDisableAutonomousResponsesAndEnableSemanticBargeIn() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildSessionConfig("fr");
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> turnDetection = (Map<String, Object>) input.get("turn_detection");
        Map<String, Object> transcription = (Map<String, Object>) input.get("transcription");
        Map<String, Object> output = (Map<String, Object>) audio.get("output");

        assertEquals("realtime", session.get("type"));
        assertEquals("gpt-realtime", session.get("model"));
        assertEquals("semantic_vad", turnDetection.get("type"));
        assertEquals("medium", turnDetection.get("eagerness"));
        assertEquals(Boolean.FALSE, turnDetection.get("create_response"));
        assertEquals(Boolean.TRUE, turnDetection.get("interrupt_response"));
        assertEquals("gpt-4o-transcribe", transcription.get("model"));
        assertEquals("fr", transcription.get("language"));
        assertEquals("near_field", ((Map<String, Object>) input.get("noise_reduction")).get("type"));
        assertEquals("marin", output.get("voice"));
        assertEquals("none", session.get("tool_choice"));
        assertTrue(session.get("instructions").toString().contains("Ne diagnostiquez jamais"));
        assertFalse(session.get("instructions").toString().contains("prescrivez un traitement"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldLocalizeRealtimeTranscriptionAndSafetyInstructions() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildSessionConfig("en-US");
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> transcription = (Map<String, Object>) input.get("transcription");

        assertEquals("en", transcription.get("language"));
        assertTrue(transcription.get("prompt").toString().contains("Preserve negations"));
        assertTrue(session.get("instructions").toString().contains("Never diagnose"));
    }

    private OpenAiRealtimeCallService service() {
        AiProperties properties = new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                "fr",
                new AiProperties.OpenAiProperties(
                        "test-key",
                        "gpt-4.1",
                        "gpt-4o-mini-transcribe",
                        "medical",
                        "https://api.openai.com/v1"),
                null,
                null);
        return new OpenAiRealtimeCallService(
                properties,
                new ObjectMapper(),
                "gpt-realtime",
                "marin",
                "gpt-4o-transcribe",
                "medium",
                "near_field");
    }
}
