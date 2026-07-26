package com.joprelys.backend.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiProviderTest {

    @Test
    void shouldSendMedicalPromptDuringTranscription() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(
                builder, "gpt-4.1", "gpt-4o-transcribe", "Contexte médical français");

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Contexte médical français")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("logprobs")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("0.8")))
                .andRespond(withSuccess("""
                        {
                          "text":"Patient stable",
                          "language":"fr",
                          "logprobs":[
                            {"token":"Patient","logprob":-0.1},
                            {"token":" stable","logprob":-0.2}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var transcription = provider.transcribeAudio("audio".getBytes(StandardCharsets.UTF_8), "audio/webm", "fr");

        assertThat(transcription.text()).isEqualTo("Patient stable");
        assertThat(transcription.locale()).isEqualTo("fr");
        assertThat(transcription.confidence()).isBetween(0.8, 1.0);
        server.verify();
    }

    @Test
    void shouldUseLowerConfidenceTailInsteadOfAverage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-4.1", "gpt-4o-transcribe", "prompt");

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andRespond(withSuccess("""
                        {
                          "text":"Dose dix milligrammes",
                          "language":"fr",
                          "logprobs":[
                            {"token":"Dose","logprob":-0.01},
                            {"token":" dix","logprob":-2.0},
                            {"token":" milligrammes","logprob":-0.01},
                            {"token":".","logprob":-0.01},
                            {"token":" ","logprob":-0.01}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        var transcription = provider.transcribeAudio("audio".getBytes(StandardCharsets.UTF_8), "audio/webm", "fr");

        assertThat(transcription.confidence()).isLessThan(0.2);
        server.verify();
    }

    @Test
    void shouldNotRequestUnsupportedLogprobsOrPromptForDiarizationModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(
                builder,
                "gpt-4.1",
                "gpt-4o-transcribe-diarize",
                "prompt that is unsupported by diarize");

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("logprobs"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("prompt that is unsupported by diarize"))))
                .andRespond(withSuccess("""
                        {"text":"Bonjour","language":"fr"}
                        """, MediaType.APPLICATION_JSON));

        var transcription = provider.transcribeAudio("audio".getBytes(StandardCharsets.UTF_8), "audio/webm", "fr");

        assertThat(transcription.confidence()).isNull();
        server.verify();
    }

    @Test
    void shouldNotSendTemperatureForGpt5ReasoningModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-5.6-terra", "gpt-4o-transcribe", "prompt");

        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("temperature"))))
                .andRespond(withSuccess(chatResponse(), MediaType.APPLICATION_JSON));

        AiChatResponse response = provider.chat(List.of(AiMessage.user("Bonjour")), "Système");

        assertThat(response.content()).isEqualTo("Réponse");
        assertThat(response.tokensUsed()).isEqualTo(12);
        server.verify();
    }

    @Test
    void shouldPreserveLegacyChatTemperatureForClassicModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-4.1", "gpt-4o-transcribe", "prompt");

        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temperature\":0.3")))
                .andRespond(withSuccess(chatResponse(), MediaType.APPLICATION_JSON));

        provider.chat(List.of(AiMessage.user("Bonjour")), "Système");

        server.verify();
    }

    @Test
    void shouldSendStrictJsonSchemaForClinicalStructuredOutput() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-4.1", "gpt-4o-transcribe", "prompt");
        Map<String, Object> schema = Map.of(
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of("facts", Map.of("type", "array")),
                "required", List.of("facts"));

        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temperature\":0.0")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"type\":\"json_schema\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"name\":\"clinical_facts_v1\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"strict\":true")))
                .andRespond(withSuccess("""
                        {
                          "model":"gpt-4.1",
                          "choices":[{"message":{"content":"{\\\"facts\\\":[]}"}}],
                          "usage":{"total_tokens":21}
                        }
                        """, MediaType.APPLICATION_JSON));

        AiChatResponse response = provider.chatStructured(
                List.of(AiMessage.user("transcript")),
                "extractor",
                "clinical_facts_v1",
                schema);

        assertThat(response.content()).isEqualTo("{\"facts\":[]}");
        assertThat(response.tokensUsed()).isEqualTo(21);
        server.verify();
    }

    @Test
    void shouldFailClosedWhenStructuredOutputIsRefused() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-4.1", "gpt-4o-transcribe", "prompt");

        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {
                          "model":"gpt-4.1",
                          "choices":[{"message":{"refusal":"cannot comply","content":null}}]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.chatStructured(
                List.of(AiMessage.user("transcript")),
                "extractor",
                "clinical_facts_v1",
                Map.of("type", "object")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_STRUCTURED_OUTPUT_REFUSED");
        server.verify();
    }

    private OpenAiProvider provider(
            RestClient.Builder builder,
            String model,
            String transcriptionModel,
            String transcriptionPrompt) {
        AiProperties.OpenAiProperties properties = new AiProperties.OpenAiProperties(
                "test-key",
                model,
                transcriptionModel,
                transcriptionPrompt,
                0.8,
                "https://api.openai.test/v1"
        );
        return new OpenAiProvider(builder.build(), properties);
    }

    private String chatResponse() {
        return """
                {
                  "model": "test-model",
                  "choices": [{"message": {"content": "Réponse"}}],
                  "usage": {"total_tokens": 12}
                }
                """;
    }
}
