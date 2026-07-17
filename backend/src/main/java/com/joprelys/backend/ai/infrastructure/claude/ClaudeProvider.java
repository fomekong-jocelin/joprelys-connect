package com.joprelys.backend.ai.infrastructure.claude;

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
 * Implémentation du fournisseur IA pour Anthropic Claude.
 *
 * <p>Claude assure la génération du brouillon clinique. Lorsqu'il est
 * explicitement sélectionné comme fournisseur de parole, la transcription est
 * déléguée à OpenAI avec le modèle configuré, car Claude ne fournit pas de STT
 * natif dans cette intégration.</p>
 */
public class ClaudeProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(ClaudeProvider.class);

    private final RestClient claudeRestClient;
    private final AiProperties.ClaudeProperties config;
    private final RestClient speechFallbackClient;
    private final AiProperties.OpenAiProperties openAiConfig;

    public ClaudeProvider(
            RestClient claudeRestClient,
            AiProperties.ClaudeProperties config,
            RestClient speechFallbackClient,
            AiProperties.OpenAiProperties openAiConfig) {
        this.claudeRestClient = claudeRestClient;
        this.config = config;
        this.speechFallbackClient = speechFallbackClient;
        this.openAiConfig = openAiConfig;
    }

    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        if (speechFallbackClient == null) {
            throw new UnsupportedOperationException(
                    "Claude ne supporte pas la transcription audio native. "
                            + "Configurez OPENAI_API_KEY pour la transcription de secours.");
        }

        log.debug("Transcription audio OpenAI de secours pour Claude "
                        + "(modèle={}, locale={}, taille={}o)",
                openAiConfig.transcribeModel(), locale, audioData.length);
        return transcribeViaOpenAi(audioData, mimeType, locale);
    }

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

    @SuppressWarnings("unchecked")
    private AiChatResponse parseMessagesResponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Claude Messages");
        }

        List<Map<String, Object>> contentBlocks =
                (List<Map<String, Object>>) response.get("content");
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            throw new IllegalStateException("Aucun contenu retourné par l'API Claude Messages");
        }

        String content = extractTextContent(contentBlocks);
        Integer tokensUsed = extractTotalTokens(response);
        String model = (String) response.get("model");
        return new AiChatResponse(content, tokensUsed, model);
    }

    private String extractTextContent(List<Map<String, Object>> contentBlocks) {
        StringBuilder result = new StringBuilder();
        for (Map<String, Object> block : contentBlocks) {
            if ("text".equals(block.get("type"))) {
                result.append(block.get("text"));
            }
        }
        return result.toString();
    }

    @SuppressWarnings("unchecked")
    private Integer extractTotalTokens(Map<String, Object> response) {
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage == null) {
            return null;
        }
        int input = usage.get("input_tokens") != null
                ? ((Number) usage.get("input_tokens")).intValue() : 0;
        int output = usage.get("output_tokens") != null
                ? ((Number) usage.get("output_tokens")).intValue() : 0;
        return input + output;
    }

    @SuppressWarnings("unchecked")
    private AiTranscription transcribeViaOpenAi(
            byte[] audioData,
            String mimeType,
            String locale) {
        String extension = resolveExtension(mimeType);
        String transcribeModel = openAiConfig != null
                && openAiConfig.transcribeModel() != null
                && !openAiConfig.transcribeModel().isBlank()
                ? openAiConfig.transcribeModel()
                : "gpt-4o-mini-transcribe";

        ByteArrayResource audioResource = new ByteArrayResource(audioData) {
            @Override
            public String getFilename() {
                return "audio." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", audioResource);
        formData.add("model", transcribeModel);
        formData.add("language", locale);
        formData.add("response_format", "verbose_json");

        Map<String, Object> response = speechFallbackClient.post()
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
