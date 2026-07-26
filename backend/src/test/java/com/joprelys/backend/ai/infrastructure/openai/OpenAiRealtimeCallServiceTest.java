package com.joprelys.backend.ai.infrastructure.openai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiRealtimeCallService.RealtimePurpose;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.ObjectMapper;

class OpenAiRealtimeCallServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldUsePatientSemanticVadForNaturalConsultationSpeech() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildSessionConfig("fr");
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> turnDetection = (Map<String, Object>) input.get("turn_detection");
        Map<String, Object> transcription = (Map<String, Object>) input.get("transcription");
        Map<String, Object> output = (Map<String, Object>) audio.get("output");

        assertEquals("realtime", session.get("type"));
        assertEquals("gpt-realtime-2.1", session.get("model"));
        assertEquals(List.of("audio"), session.get("output_modalities"));
        assertEquals(
                List.of("item.input_audio_transcription.logprobs"),
                session.get("include"));
        assertEquals("semantic_vad", turnDetection.get("type"));
        assertEquals("low", turnDetection.get("eagerness"));
        assertEquals(Boolean.FALSE, turnDetection.get("create_response"));
        assertEquals(Boolean.TRUE, turnDetection.get("interrupt_response"));
        assertEquals("gpt-4o-transcribe", transcription.get("model"));
        assertEquals("fr", transcription.get("language"));
        assertTrue(transcription.get("prompt").toString().contains("texte vide"));
        assertEquals("near_field", ((Map<String, Object>) input.get("noise_reduction")).get("type"));
        assertEquals("marin", output.get("voice"));
        assertEquals("none", session.get("tool_choice"));
        assertTrue(session.get("instructions").toString().contains("Ne diagnostiquez jamais"));
        assertFalse(session.get("instructions").toString().contains("prescrivez un traitement"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepVitalsSemanticVadResponsive() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildSessionConfig("fr", RealtimePurpose.VITALS);
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> turnDetection = (Map<String, Object>) input.get("turn_detection");

        assertEquals("semantic_vad", turnDetection.get("type"));
        assertEquals("medium", turnDetection.get("eagerness"));
        assertEquals(Boolean.FALSE, turnDetection.get("create_response"));
        assertEquals(Boolean.TRUE, turnDetection.get("interrupt_response"));
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

    @Test
    @SuppressWarnings("unchecked")
    void shouldUseLongerServerVadSilenceForConsultationCompatibilityMode() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildCompatibilitySessionConfig(
                "fr", "gpt-realtime-2.1", RealtimePurpose.CONSULTATION);
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> turnDetection = (Map<String, Object>) input.get("turn_detection");

        assertEquals("server_vad", turnDetection.get("type"));
        assertEquals(0.8, turnDetection.get("threshold"));
        assertEquals(1200, turnDetection.get("silence_duration_ms"));
        assertEquals(500, turnDetection.get("prefix_padding_ms"));
        assertEquals(Boolean.FALSE, turnDetection.get("create_response"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepVitalsServerVadCompatibilityModeFast() {
        OpenAiRealtimeCallService service = service();

        Map<String, Object> session = service.buildCompatibilitySessionConfig(
                "fr", "gpt-realtime-2.1", RealtimePurpose.VITALS);
        Map<String, Object> audio = (Map<String, Object>) session.get("audio");
        Map<String, Object> input = (Map<String, Object>) audio.get("input");
        Map<String, Object> turnDetection = (Map<String, Object>) input.get("turn_detection");

        assertEquals("server_vad", turnDetection.get("type"));
        assertEquals(0.8, turnDetection.get("threshold"));
        assertEquals(650, turnDetection.get("silence_duration_ms"));
        assertEquals(300, turnDetection.get("prefix_padding_ms"));
        assertEquals(Boolean.FALSE, turnDetection.get("create_response"));
    }

    @Test
    void shouldPreserveRawSdpAndMultipartContentTypes() {
        OpenAiRealtimeCallService service = service();
        String sdp = "v=0\r\n"
                + "o=- 123 456 IN IP4 127.0.0.1\r\n"
                + "s=-\r\n"
                + "t=0 0\r\n"
                + "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n";

        MultiValueMap<String, Object> multipart = service.buildMultipartBody(
                sdp,
                service.buildSessionConfig("fr"));

        HttpEntity<?> sdpPart = assertInstanceOf(HttpEntity.class, multipart.getFirst("sdp"));
        byte[] sdpBytes = assertInstanceOf(byte[].class, sdpPart.getBody());
        assertArrayEquals(sdp.getBytes(StandardCharsets.UTF_8), sdpBytes);
        assertEquals(MediaType.valueOf("application/sdp"), sdpPart.getHeaders().getContentType());

        HttpEntity<?> sessionPart = assertInstanceOf(HttpEntity.class, multipart.getFirst("session"));
        byte[] sessionBytes = assertInstanceOf(byte[].class, sessionPart.getBody());
        String sessionJson = new String(sessionBytes, StandardCharsets.UTF_8);
        assertEquals(MediaType.APPLICATION_JSON, sessionPart.getHeaders().getContentType());
        assertTrue(sessionJson.contains("\"type\":\"realtime\""));
        assertTrue(sessionJson.contains("\"model\":\"gpt-realtime-2.1\""));
    }

    private OpenAiRealtimeCallService service() {
        AiProperties properties = new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                0.35,
                "fr",
                new AiProperties.OpenAiProperties(
                        "test-key",
                        "gpt-4.1",
                        "gpt-4o-mini-transcribe",
                        "medical",
                        0.8,
                        "https://api.openai.com/v1"),
                null,
                null);
        return new OpenAiRealtimeCallService(
                properties,
                new ObjectMapper(),
                "gpt-realtime-2.1",
                "gpt-realtime-2.1-mini",
                "marin",
                "gpt-4o-transcribe",
                "medium",
                "near_field");
    }
}
