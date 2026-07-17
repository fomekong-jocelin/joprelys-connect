package com.joprelys.backend.ai.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
        OpenAiProvider provider = provider(builder, "gpt-4.1", "Contexte médical français");

        server.expect(requestTo("https://api.openai.test/v1/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Contexte médical français")))
                .andRespond(withSuccess("{\"text\":\"Patient stable\",\"language\":\"fr\"}", MediaType.APPLICATION_JSON));

        var transcription = provider.transcribeAudio("audio".getBytes(StandardCharsets.UTF_8), "audio/webm", "fr");

        assertThat(transcription.text()).isEqualTo("Patient stable");
        assertThat(transcription.locale()).isEqualTo("fr");
        server.verify();
    }

    @Test
    void shouldNotSendTemperatureForGpt5ReasoningModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-5.6-terra", "prompt");

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
    void shouldSendTemperatureForClassicModel() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.test/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiProvider provider = provider(builder, "gpt-4.1", "prompt");

        server.expect(requestTo("https://api.openai.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temperature\":0.3")))
                .andRespond(withSuccess(chatResponse(), MediaType.APPLICATION_JSON));

        provider.chat(List.of(AiMessage.user("Bonjour")), "Système");

        server.verify();
    }

    private OpenAiProvider provider(RestClient.Builder builder, String model, String transcriptionPrompt) {
        AiProperties.OpenAiProperties properties = new AiProperties.OpenAiProperties(
                "test-key",
                model,
                "gpt-4o-transcribe",
                transcriptionPrompt,
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
