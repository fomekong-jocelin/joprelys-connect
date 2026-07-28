package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class OpenAiSpeechSynthesisService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiSpeechSynthesisService.class);
    private static final int MAX_TEXT_LENGTH = 4096;

    private final RestClient restClient;
    private final String model;
    private final String voice;
    private final String instructions;

    public OpenAiSpeechSynthesisService(
            AiProperties properties,
            @Value("${joprelys.ai.openai.tts-model:tts-1}") String model,
            @Value("${joprelys.ai.openai.tts-voice:marin}") String voice,
            @Value("${joprelys.ai.openai.tts-instructions:Voix médicale professionnelle, calme, chaleureuse, concise et naturelle. Prononcer clairement les nombres, unités et noms de médicaments sans dramatiser.}") String instructions) {
        AiProperties.OpenAiProperties openAi = properties.openai();
        this.restClient = openAi == null
                || openAi.apiKey() == null
                || openAi.apiKey().isBlank()
                || openAi.baseUrl() == null
                || openAi.baseUrl().isBlank()
                ? null
                : RestClient.builder()
                        .baseUrl(openAi.baseUrl())
                        .defaultHeader("Authorization", "Bearer " + openAi.apiKey())
                        .build();
        this.model = normalize(model, "tts-1");
        this.voice = normalize(voice, "marin");
        this.instructions = instructions == null ? "" : instructions.trim();
    }

    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_SPEECH_TEXT_INVALID");
        }
        if (restClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SPEECH_NOT_CONFIGURED");
        }
        String normalized = text.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            normalized = normalized.substring(0, MAX_TEXT_LENGTH);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("voice", voice);
        body.put("input", normalized);
        body.put("response_format", "mp3");
        if (supportsInstructions(model) && !instructions.isBlank()) {
            body.put("instructions", instructions);
        }

        try {
            byte[] audio = restClient.post()
                    .uri("/audio/speech")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
            if (audio == null || audio.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI_SPEECH_EMPTY");
            }
            log.info(
                    "AI_USAGE provider=openai operation=tts model={} inputChars={} outputBytes={}",
                    model,
                    normalized.length(),
                    audio.length);
            return audio;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Échec synthèse vocale OpenAI status={}", exception.getStatusCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SPEECH_UNAVAILABLE");
        } catch (RuntimeException exception) {
            log.warn("Échec synthèse vocale OpenAI", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_SPEECH_UNAVAILABLE");
        }
    }

    private boolean supportsInstructions(String selectedModel) {
        String normalized = selectedModel == null ? "" : selectedModel.toLowerCase();
        return !normalized.equals("tts-1") && !normalized.equals("tts-1-hd");
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
