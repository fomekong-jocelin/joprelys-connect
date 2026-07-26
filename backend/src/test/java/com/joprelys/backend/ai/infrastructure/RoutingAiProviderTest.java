package com.joprelys.backend.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutingAiProviderTest {

    @Test
    void givenDifferentProviders_whenTranscribing_thenUsesSpeechProviderOnly() {
        TrackingProvider speech = new TrackingProvider("transcription", "speech-model");
        TrackingProvider draft = new TrackingProvider("draft", "draft-model");
        RoutingAiProvider routing = new RoutingAiProvider(speech, draft);

        AiTranscription result = routing.transcribeAudio(
                new byte[]{1, 2, 3},
                "audio/webm",
                "fr");

        assertThat(result.text()).isEqualTo("transcription");
        assertThat(speech.transcriptionCalls).isEqualTo(1);
        assertThat(draft.transcriptionCalls).isZero();
        assertThat(speech.chatCalls).isZero();
    }

    @Test
    void givenDifferentProviders_whenGeneratingDraft_thenUsesDraftProviderOnly() {
        TrackingProvider speech = new TrackingProvider("transcription", "speech-model");
        TrackingProvider draft = new TrackingProvider("draft", "draft-model");
        RoutingAiProvider routing = new RoutingAiProvider(speech, draft);

        AiChatResponse result = routing.chat(
                List.of(AiMessage.user("Le patient présente une toux sèche.")),
                "Produis uniquement un brouillon clinique.");

        assertThat(result.content()).isEqualTo("draft");
        assertThat(result.model()).isEqualTo("draft-model");
        assertThat(draft.chatCalls).isEqualTo(1);
        assertThat(speech.chatCalls).isZero();
        assertThat(draft.transcriptionCalls).isZero();
    }

    @Test
    void givenDifferentProviders_whenExtractingStructuredFacts_thenUsesDraftProviderOnly() {
        TrackingProvider speech = new TrackingProvider("transcription", "speech-model");
        TrackingProvider draft = new TrackingProvider("{\"facts\":[]}", "draft-model");
        RoutingAiProvider routing = new RoutingAiProvider(speech, draft);

        AiChatResponse result = routing.chatStructured(
                List.of(AiMessage.user("transcript")),
                "extractor",
                "clinical_facts_v1",
                Map.of("type", "object"));

        assertThat(result.content()).isEqualTo("{\"facts\":[]}");
        assertThat(result.model()).isEqualTo("draft-model");
        assertThat(draft.structuredCalls).isEqualTo(1);
        assertThat(speech.structuredCalls).isZero();
    }

    private static final class TrackingProvider implements AiProvider {

        private final String content;
        private final String model;
        private int transcriptionCalls;
        private int chatCalls;
        private int structuredCalls;

        private TrackingProvider(String content, String model) {
            this.content = content;
            this.model = model;
        }

        @Override
        public AiTranscription transcribeAudio(
                byte[] audioData,
                String mimeType,
                String locale) {
            transcriptionCalls++;
            return new AiTranscription(content, locale, null);
        }

        @Override
        public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
            chatCalls++;
            return new AiChatResponse(content, null, model);
        }

        @Override
        public AiChatResponse chatStructured(
                List<AiMessage> messages,
                String systemPrompt,
                String schemaName,
                Map<String, Object> schema) {
            structuredCalls++;
            return new AiChatResponse(content, null, model);
        }
    }
}
