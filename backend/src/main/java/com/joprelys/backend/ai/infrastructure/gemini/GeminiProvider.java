package com.joprelys.backend.ai.infrastructure.gemini;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Implémentation du fournisseur IA pour Google Gemini.
 *
 * <p>Utilise l'API Generative Language (generateContent) pour le chat
 * et la transcription audio. Gemini étant multimodal, l'audio est envoyé
 * en inline data (base64) avec une instruction de transcription.</p>
 */
public class GeminiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private static final String TRANSCRIPTION_INSTRUCTION =
            "Transcris fidèlement le contenu audio suivant en texte. "
            + "Retourne uniquement le texte transcrit, sans commentaire ni formatage supplémentaire. "
            + "La langue attendue est : %s.";

    private final RestClient restClient;
    private final AiProperties.GeminiProperties config;

    public GeminiProvider(RestClient restClient, AiProperties.GeminiProperties config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public AiTranscription transcribeAudio(byte[] audioData, String mimeType, String locale) {
        log.debug("Transcription audio via Gemini (modèle={}, locale={}, taille={}o)",
                config.model(), locale, audioData.length);

        String base64Audio = Base64.getEncoder().encodeToString(audioData);
        String instruction = String.format(TRANSCRIPTION_INSTRUCTION, locale);
        Map<String, Object> requestBody = buildTranscriptionRequest(base64Audio, mimeType, instruction);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", config.model(), config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        String text = extractTextFromResponse(response);
        return new AiTranscription(text, locale, null);
    }

    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        log.debug("Chat via Gemini (modèle={}, messages={})", config.model(), messages.size());

        Map<String, Object> requestBody = buildChatRequest(messages, systemPrompt);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/models/{model}:generateContent?key={key}", config.model(), config.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        return parseChatResponse(response);
    }

    private Map<String, Object> buildTranscriptionRequest(
            String base64Audio,
            String mimeType,
            String instruction) {
        Map<String, Object> inlineData = Map.of(
                "mimeType", mimeType != null ? mimeType : "audio/wav",
                "data", base64Audio);
        Map<String, Object> audioPart = Map.of("inlineData", inlineData);
        Map<String, Object> textPart = Map.of("text", instruction);
        Map<String, Object> content = Map.of("parts", List.of(audioPart, textPart));
        return Map.of("contents", List.of(content));
    }

    private Map<String, Object> buildChatRequest(
            List<AiMessage> messages,
            String systemPrompt) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (AiMessage message : messages) {
            if (message.role() == AiMessage.Role.SYSTEM) {
                continue;
            }
            String role = message.role() == AiMessage.Role.USER ? "user" : "model";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", message.content()))));
        }

        String dynamicSystem = messages.stream()
                .filter(message -> message.role() == AiMessage.Role.SYSTEM)
                .map(AiMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .collect(Collectors.joining("\n\n"));
        String effectiveSystemPrompt = dynamicSystem.isBlank()
                ? systemPrompt
                : systemPrompt + "\n\n" + dynamicSystem;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        requestBody.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", effectiveSystemPrompt))));
        requestBody.put("generationConfig", Map.of(
                "temperature", 0.3,
                "responseMimeType", "text/plain"));
        return requestBody;
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API Gemini");
        }
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Aucun candidat retourné par l'API Gemini");
        }
        Map<String, Object> content =
                (Map<String, Object>) candidates.getFirst().get("content");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");
        return (String) parts.getFirst().get("text");
    }

    @SuppressWarnings("unchecked")
    private AiChatResponse parseChatResponse(Map<String, Object> response) {
        String text = extractTextFromResponse(response);
        Integer tokensUsed = null;
        Map<String, Object> usageMetadata =
                (Map<String, Object>) response.get("usageMetadata");
        if (usageMetadata != null && usageMetadata.get("totalTokenCount") != null) {
            tokensUsed = ((Number) usageMetadata.get("totalTokenCount")).intValue();
        }
        return new AiChatResponse(text, tokensUsed, config.model());
    }
}
