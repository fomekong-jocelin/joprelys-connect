package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        if (supportsTranscriptionLogprobs(config.transcribeModel())) {
            formData.add("include[]", "logprobs");
        }
        if (config.transcribeVadThreshold() > 0.0
                && config.transcribeVadThreshold() <= 1.0) {
            formData.add("threshold", config.transcribeVadThreshold());
        }
        if (config.transcribePrompt() != null && !config.transcribePrompt().isBlank()
                && !isDiarizationModel(config.transcribeModel())) {
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
        return new AiTranscription(text, detectedLanguage, extractConfidence(response));
    }

    @Override
    public AiChatResponse chat(List<AiMessage> messages, String systemPrompt) {
        log.debug("Chat via OpenAI (modèle={}, messages={})", config.model(), messages.size());

        List<Map<String, String>> apiMessages = buildApiMessages(messages, systemPrompt);
        Map<String, Object> requestBody = new java.util.HashMap<>(Map.of(
                "model", config.model(),
                "messages", apiMessages));
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

    private boolean supportsCustomTemperature(String model) {
        if (model == null) {
            return true;
        }
        String normalized = model.toLowerCase();
        return !normalized.startsWith("gpt-5") && !normalized.startsWith("o1")
                && !normalized.startsWith("o3") && !normalized.startsWith("o4");
    }

    private boolean supportsTranscriptionLogprobs(String model) {
        if (model == null || isDiarizationModel(model)) {
            return false;
        }
        String normalized = model.toLowerCase();
        return normalized.startsWith("gpt-4o-transcribe")
                || normalized.startsWith("gpt-4o-mini-transcribe");
    }

    private boolean isDiarizationModel(String model) {
        return model != null && model.toLowerCase().contains("transcribe-diarize");
    }

    private Double extractConfidence(Map<String, Object> response) {
        Object rawLogprobs = response.get("logprobs");
        if (!(rawLogprobs instanceof List<?> logprobs) || logprobs.isEmpty()) {
            return null;
        }
        List<Double> probabilities = new ArrayList<>();
        for (Object rawEntry : logprobs) {
            if (!(rawEntry instanceof Map<?, ?> entry)
                    || !(entry.get("logprob") instanceof Number logprob)) {
                continue;
            }
            probabilities.add(Math.exp(Math.max(-20.0, logprob.doubleValue())));
        }
        if (probabilities.isEmpty()) {
            return null;
        }
        probabilities.sort(Double::compareTo);
        int lowerQuintileIndex = (int) Math.floor((probabilities.size() - 1) * 0.2);
        return probabilities.get(lowerQuintileIndex);
    }

    private List<Map<String, String>> buildApiMessages(
            List<AiMessage> messages,
            String systemPrompt) {
        String dynamicSystem = messages.stream()
                .filter(message -> message.role() == AiMessage.Role.SYSTEM)
                .map(AiMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .collect(Collectors.joining("\n\n"));
        String effectiveSystemPrompt = dynamicSystem.isBlank()
                ? systemPrompt
                : systemPrompt + "\n\n" + dynamicSystem;

        List<Map<String, String>> apiMessages = new ArrayList<>();
        apiMessages.add(Map.of("role", "system", "content", effectiveSystemPrompt));
        for (AiMessage message : messages) {
            if (message.role() == AiMessage.Role.SYSTEM) {
                continue;
            }
            String role = switch (message.role()) {
                case USER -> "user";
                case ASSISTANT -> "assistant";
                case SYSTEM -> throw new IllegalStateException("SYSTEM_ALREADY_HANDLED");
            };
            apiMessages.add(Map.of("role", role, "content", message.content()));
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
