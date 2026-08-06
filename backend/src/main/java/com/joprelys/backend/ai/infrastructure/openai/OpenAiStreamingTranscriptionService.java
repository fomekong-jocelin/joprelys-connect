package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service de transcription streaming via OpenAI.
 *
 * <p>L'appel HTTP est bloquant. Il est donc exécuté sur le scheduler borné
 * élastique afin de ne jamais bloquer le thread qui reçoit les paquets
 * WebSocket du mobile.</p>
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class OpenAiStreamingTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiStreamingTranscriptionService.class);

    private final RestClient restClient;
    private final AiProperties.OpenAiProperties config;

    public OpenAiStreamingTranscriptionService(AiProperties properties) {
        this.config = properties.openai();
        this.restClient = isConfigured(config)
                ? RestClient.builder()
                        .baseUrl(config.baseUrl())
                        .defaultHeader("Authorization", "Bearer " + config.apiKey())
                        .build()
                : null;
    }

    public Mono<AiTranscription> transcribe(byte[] audioData, String mimeType, String locale) {
        return Mono.fromCallable(() -> transcribeBlocking(audioData, mimeType, locale))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AiTranscription transcribeBlocking(
            byte[] audioData,
            String mimeType,
            String locale) {
        if (audioData == null || audioData.length == 0) {
            log.warn("Chunk audio vide, skip transcription");
            return new AiTranscription("", locale, 1.0);
        }
        if (restClient == null || config == null || isBlank(config.transcribeModel())) {
            throw new IllegalStateException(
                    "Configuration OpenAI indisponible pour la transcription streaming");
        }

        byte[] processedAudio = audioData;
        String actualMimeType = mimeType;
        if ("audio/pcm".equalsIgnoreCase(mimeType) && !WavEncoder.hasWavHeader(audioData)) {
            log.debug("Encodage PCM vers WAV ({} octets)", audioData.length);
            processedAudio = WavEncoder.encodePcm16MonoToWav(audioData, 16_000);
            actualMimeType = "audio/wav";
        }

        log.debug(
                "Transcription streaming chunk taille={} type={} locale={}",
                processedAudio.length,
                actualMimeType,
                locale);

        String extension = resolveExtension(actualMimeType);
        byte[] finalAudio = processedAudio;
        ByteArrayResource audioResource = new ByteArrayResource(finalAudio) {
            @Override
            public String getFilename() {
                return "chunk." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", audioResource);
        formData.add("model", config.transcribeModel());
        formData.add("language", locale);
        formData.add("response_format", "json");
        formData.add("temperature", 0.0);

        if (locale != null && locale.equalsIgnoreCase("fr")) {
            formData.add(
                    "prompt",
                    "Dictée médicale clinique : consultation, examen, diagnostic, "
                            + "antécédents, traitement, constantes vitales, auscultation, palpation.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(formData)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Réponse vide de l'API de transcription");
        }

        String text = response.get("text") == null ? "" : response.get("text").toString();
        String detectedLanguage = response.get("language") == null
                ? locale
                : response.get("language").toString();

        log.debug(
                "Transcription reçue text=\"{}\" audio={} octets",
                text.substring(0, Math.min(50, text.length())),
                audioData.length);

        return new AiTranscription(
                text.trim(),
                detectedLanguage,
                extractConfidence(response));
    }

    private boolean isConfigured(AiProperties.OpenAiProperties openAi) {
        return openAi != null
                && !isBlank(openAi.apiKey())
                && !isBlank(openAi.baseUrl());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveExtension(String mimeType) {
        if (mimeType == null) {
            return "wav";
        }
        return switch (mimeType.toLowerCase()) {
            case "audio/webm" -> "webm";
            case "audio/mp4", "audio/m4a" -> "m4a";
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav", "audio/wave", "audio/pcm" -> "wav";
            case "audio/ogg" -> "ogg";
            default -> "wav";
        };
    }

    private Double extractConfidence(Map<String, Object> response) {
        return 0.95;
    }
}
