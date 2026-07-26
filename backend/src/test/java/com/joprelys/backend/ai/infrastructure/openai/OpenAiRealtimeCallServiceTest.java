package com.joprelys.backend.ai.infrastructure.openai;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
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
        assertEquals(java.util.List.of("audio"), session.get("output_modalities"));
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

    @Test
    void shouldSendSdpAndSessionAsMultipartFormData() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiRealtimeCallService service = service(builder.build());

        server.expect(requestTo("https://api.openai.test/v1/realtime/calls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Accept", containsString("application/sdp")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(content().string(containsString("name=\"sdp\"")))
                .andExpect(content().string(containsString("application/sdp")))
                .andExpect(content().string(containsString("name=\"session\"")))
                .andExpect(content().string(containsString("\"type\":\"realtime\"")))
                .andExpect(content().string(containsString("\"model\":\"gpt-realtime\"")))
                .andRespond(withSuccess("v=0\r\ns=answer\r\n", MediaType.valueOf("application/sdp")));

        String answer = service.createCall("v=0\r\ns=offer\r\n", "fr");

        assertTrue(answer.contains("s=answer"));
        server.verify();
    }

    @Test
    void shouldExposeQuotaOrBudgetFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiRealtimeCallService service = service(builder.build());

        server.expect(requestTo("https://api.openai.test/v1/realtime/calls"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"quota exceeded\"}}"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createCall("v=0\r\ns=offer\r\n", "fr"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("AI_REALTIME_QUOTA_OR_BUDGET", exception.getReason());
        server.verify();
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

    private OpenAiRealtimeCallService service(RestClient restClient) {
        return new OpenAiRealtimeCallService(
                restClient,
                new ObjectMapper(),
                "gpt-realtime",
                "marin",
                "gpt-4o-transcribe",
                "medium",
                "near_field");
    }
}
