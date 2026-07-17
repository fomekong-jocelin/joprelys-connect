package com.joprelys.backend.ai.infrastructure.claude;

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
 * Implémentation du fournisseur IA pour Anthropic Claude.
 *
 * <p>Utilise l'API Messages d'Anthropic pour le chat. Pour la transcription
 * audio, Claude ne possède pas de capacité STT native : un fallback vers
 * OpenAI Whisper est utilisé si une clé API OpenAI est configurée.</p>
 */
public class ClaudeProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);

    private final RestClient claudeRestClient;
    private final AiProperties.ClaudeProperties config;
    private final RestClient whisperFallbackClient;
    private final AiProperties.OpenAiProperties openAiConfig;

    /**
     * Construit une instance du fournisseur Claude.
     *
     * @param claudeRestClient      le client HTTP pré-configuré pour l'API Anthropic
     * @param config                la configuration spécifique à Claude
     * @param whisperFallbackClient le client HTTP pour Whisper (fallback STT), peut être null
     * @param openAiConfig          la configuration OpenAI pour le fallback Whisper, peut être null
     */
    public ClaudeProvider(
            RestClient claudeRestClient,
            AiProperties.ClaudeProperties config,
            RestClient whisperFallbackClient,
            AiProperties.OpenAiProperties openAiConfig) {
        this.claudeRestClient = claudeRestClient;
        this.config = config;
        this.whisperFallbackClient = whisperFallbackClient;
        this.openAiConfig = openAiConfig;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Claude ne possède pas de capacité STT native. Si une clé API OpenAI
     * est configurée, la transcription est effectuée via OpenAI Whisper en fallback.
     * Sinon, une {@link UnsupportedOperationException} est levée.</p>
     */
    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        if (whisperFallbackClient == null) {
            throw new UnsupportedOperationException(
                    "Claude ne supporte pas la transcription audio native. "
                    + "Configurez une clé API OpenAI (OPENAI_API_KEY) pour utiliser Whisper en fallback.");
        }

        log.debug("Transcription audio via Whisper (fallback Claude, locale={}, taille={}o)",
                locale, audioData.length);
        return transcribeViaWhisperFallback(audioData, mimeType, locale);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Utilise l'API Messages d'Anthropic (POST /messages) avec les en-têtes
     * {@code x-api-key} et {@code anthropic-version} pré-configurés.</p>
     */
    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        log.debug("Chat via Claude (modèle={}, messages={})", config.model(), messages.size());

        List<Map<String, String>> apiMessages = buildApiMessages(messages);

        Map<String, Object> requestBody = Map.of(
                "model", config.model(),
                "max_tokens", 4096,
                "system", systemPrompt,
                "messages", apiMessages
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = claudeRestClient.post()
                .uri("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return parseMessagesResponse(response);
    }

    /**
     * Construit la liste des messages au format Anthropic (exclut les messages système).
     */
    private List<Map<String, String>> buildApiMessages(List<AiMessage> messages) {
        List<Map<String, String>> apiMessages = new ArrayList<>();

        for (AiMessage msg : messages) {
            if (msg.role() == AiMessage.Role.SYSTEM) {
                continue;
            }
            String role = msg.role() == AiMessage.Role.USER ? "user" : "assistant";
            apiMessages.add(Map.of("role", role, "content", msg.content()));
        }

        return apiMessages;
    }

    /**
     * Parse la réponse de l'API Messages d'Anthropic.
     */
    @SuppressWarnings("unchecked")
    private AiChatResponse parseMessagesResponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Claude Messages");
        }

        List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            throw new IllegalStateException("Aucun contenu retourné par l'API Claude Messages");
        }

        String content = extractTextContent(contentBlocks);
        Integer tokensUsed = extractTotalTokens(response);
        String model = (String) response.get("model");

        return new AiChatResponse(content, tokensUsed, model);
    }

    /**
     * Extrait le contenu textuel des blocs de contenu Anthropic.
     */
    private String extractTextContent(List<Map<String, Object>> contentBlocks) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> block : contentBlocks) {
            if ("text".equals(block.get("type"))) {
                sb.append(block.get("text"));
            }
        }
        return sb.toString();
    }

    /**
     * Extrait le nombre total de tokens (entrée + sortie) depuis la réponse Anthropic.
     */
    @SuppressWarnings("unchecked")
    private Integer extractTotalTokens(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
            int input = usage.get("input_tokens") != null ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int output = usage.get("output_tokens") != null ? ((Number) usage.get("output_tokens")).intValue() : 0;
            return input + output;
        }
        return null;
    }

    /**
     * Effectue la transcription audio via OpenAI Whisper en fallback.
     */
    @SuppressWarnings("unchecked")
    private AiTranscription transcribeViaWhisperFallback(byte[] audioData, String mimeType, String locale) {
        String extension = resolveExtension(mimeType);
        String whisperModel = openAiConfig != null && openAiConfig.whisperModel() != null
                ? openAiConfig.whisperModel() : "whisper-1";

        ByteArrayResource audioResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return "audio." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", audioResource);
        formData.add("model", whisperModel);
        formData.add("language", locale);
        formData.add("response_format", "verbose_json");

        Map<String, Object> response = whisperFallbackClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Whisper (fallback)");
        }

        String text = (String) response.get("text");
        String detectedLanguage = (String) response.getOrDefault("language", locale);
        return new AiTranscription(text, detectedLanguage, null);
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
