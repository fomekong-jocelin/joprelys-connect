package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du fournisseur IA pour OpenAI.
 *
 * <p>Utilise l'API Whisper pour la transcription audio (Speech-to-Text)
 * et l'API Chat Completions pour les conversations. Communique via
 * {@link RestClient} sans dépendance externe sur un SDK OpenAI.</p>
 */
public class OpenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private final RestClient restClient;
    private final AiProperties.OpenAiProperties config;

    /**
     * Construit une instance du fournisseur OpenAI.
     *
     * @param restClient le client HTTP pré-configuré avec l'URL de base et l'autorisation
     * @param config     la configuration spécifique à OpenAI
     */
    public OpenAiProvider(RestClient restClient, AiProperties.OpenAiProperties config) {
        this.restClient = restClient;
        this.config = config;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utilise l'API OpenAI Whisper (POST /audio/transcriptions) pour transcrire
     * l'audio en texte. Le fichier audio est envoyé en multipart/form-data.</p>
     */
    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        log.debug("Transcription audio via OpenAI Whisper (modèle={}, locale={}, taille={}o)",
                config.whisperModel(), locale, audioData.length);

        String extension = resolveExtension(mimeType);
        ByteArrayResource audioResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return "audio." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", audioResource);
        formData.add("model", config.whisperModel());
        formData.add("language", locale);
        formData.add("response_format", "verbose_json");

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Whisper");
        }

        String text = (String) response.get("text");
        String detectedLanguage = (String) response.getOrDefault("language", locale);
        return new AiTranscription(text, detectedLanguage, null);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utilise l'API Chat Completions (POST /chat/completions) d'OpenAI.
     * Le prompt système est ajouté en tête de la liste des messages.</p>
     */
    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        log.debug("Chat via OpenAI (modèle={}, messages={})", config.model(), messages.size());

        List<Map<String, String>> apiMessages = buildApiMessages(messages, systemPrompt);

        Map<String, Object> requestBody = Map.of(
                "model", config.model(),
                "messages", apiMessages,
                "temperature", 0.3
        );

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
     * Construit la liste des messages au format attendu par l'API OpenAI.
     */
    private List<Map<String, String>> buildApiMessages(List<AiMessage> messages, String systemPrompt) {
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

    /**
     * Parse la réponse de l'API Chat Completions.
     */
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

    /**
     * Extrait le nombre total de tokens depuis la réponse.
     */
    @SuppressWarnings("unchecked")
    private Integer extractTotalTokens(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null && usage.get("total_tokens") != null) {
            return ((Number) usage.get("total_tokens")).intValue();
        }
        return null;
    }

    /**
     * Résout l'extension de fichier à partir du type MIME audio.
     */
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
