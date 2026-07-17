package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Implémentation du fournisseur IA pour OpenAI.
 *
 * <p>Utilise l'API de transcription audio et l'API Chat Completions sans
 * dépendance au SDK OpenAI.</p>
 */
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final RestClient restClient;
    private final AiProperties.OpenAiProperties config;

    public OpenAiProvider(RestClient restClient, AiProperties.OpenAiProperties config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        log.debug("Transcription audio via OpenAI (modèle={}, locale={}, taille={}o)",
                config.transcribeModel(), locale, audioData.length);

        String extension = resolveExtension(mimeType);
        ByteArrayResource audioResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return "audio." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", audioResource);
        formData.add("model", config.transcribeModel());
        formData.add("language", locale);
        formData.add("response_format", "json");
        if (config.transcribePrompt() != null && !config.transcribePrompt().isBlank()) {
            formData.add("prompt", config.transcribePrompt());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API de transcription OpenAI");
        }

        String text = (String) response.get("text");
        String detectedLanguage = (String) response.getOrDefault("language", locale);
        return new AiTranscription(text, detectedLanguage, null);
    }

    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        log.debug("Chat via OpenAI (modèle={}, messages={})", config.model(), messages.size());

        List<Map<String, String>> apiMessages = buildApiMessages(messages, systemPrompt);
        Map<String, Object> requestBody = new java.util.HashMap<>(Map.of(
                "model", config.model(),
                "messages", apiMessages
        ));
        if (supportsCustomTemperature(config.model())) {
            requestBody.put("temperature", 0.3);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return parseCompletionResponse(response);
    }

    /**
     * Les modèles à raisonnement de la famille GPT-5.x (gpt-5, gpt-5.6-*, o-series)
     * refusent une {@code temperature} personnalisée : seule la valeur par défaut
     * est acceptée par l'API.
     */
    private boolean supportsCustomTemperature(String model) {
        if (model == null) {
            return true;
        }
        String normalized = model.toLowerCase();
        return !normalized.startsWith("gpt-5") && !normalized.startsWith("o1")
                && !normalized.startsWith("o3") && !normalized.startsWith("o4");
    }

    private List<Map<String, String>> buildApiMessages(
            List<AiMessage> messages,
            String systemPrompt) {
        List<Map<String, String>> apiMessages = new ArrayList<>();
        apiMessages.add(Map.of("role", "system", "content", systemPrompt));

        for (AiMessage msg : messages) {
            String role = switch (msg.role()) {
                case SYSTEM -> "system";
                case USER -> "user";
                case ASSISTANT -> "assistant";
            };
            apiMessages.add(Map.of("role", role, "content", msg.content()));
        }
        return apiMessages;
    }

    @SuppressWarnings("unchecked")
    private AiChatResponse parseCompletionResponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Chat Completions");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Aucun choix retourné par l'API Chat Completions");
        }

        Map<String, Object> message = (Map<String, Object>) choices.getFirst().get("message");
        String content = (String) message.get("content");
        Integer tokensUsed = extractTotalTokens(response);
        String model = (String) response.get("model");
        return new AiChatResponse(content, tokensUsed, model);
    }

    @SuppressWarnings("unchecked")
    private Integer extractTotalTokens(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null && usage.get("total_tokens") != null) {
            return ((Number) usage.get("total_tokens")).intValue();
        }
        return null;
    }

    private String resolveExtension(String mimeType) {
        if (mimeType == null) {
            return "wav";
        }
        return switch (mimeType.toLowerCase()) {
            case "audio/webm" -> "webm";
            case "audio/mp3", "audio/mpeg" -> "mp3";
            case "audio/mp4", "audio/m4a" -> "m4a";
            case "audio/ogg" -> "ogg";
            case "audio/flac" -> "flac";
            default -> "wav";
        };
    }
}
