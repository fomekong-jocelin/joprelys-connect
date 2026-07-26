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
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class OpenAiRealtimeCallService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiRealtimeCallService.class);
    private static final int MAX_SDP_LENGTH = 128_000;
    private static final MediaType APPLICATION_SDP = MediaType.valueOf("application/sdp");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final String voice;
    private final String transcriptionModel;
    private final String vadEagerness;
    private final String noiseReduction;

    public OpenAiRealtimeCallService(
            AiProperties properties,
            ObjectMapper objectMapper,
            @Value("${joprelys.ai.openai.realtime-model:gpt-realtime}") String model,
            @Value("${joprelys.ai.openai.realtime-voice:marin}") String voice,
            @Value("${joprelys.ai.openai.realtime-transcribe-model:gpt-4o-transcribe}") String transcriptionModel,
            @Value("${joprelys.ai.openai.realtime-vad-eagerness:medium}") String vadEagerness,
            @Value("${joprelys.ai.openai.realtime-noise-reduction:near_field}") String noiseReduction) {
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
        this.objectMapper = objectMapper;
        this.model = model;
        this.voice = voice;
        this.transcriptionModel = transcriptionModel;
        this.vadEagerness = normalizeEagerness(vadEagerness);
        this.noiseReduction = normalizeNoiseReduction(noiseReduction);
    }

    public String createCall(String sdpOffer, String locale) {
        if (sdpOffer == null || sdpOffer.isBlank() || sdpOffer.length() > MAX_SDP_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_SDP_INVALID");
        }
        if (restClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_NOT_CONFIGURED");
        }

        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("sdp", sdpOffer.trim()).contentType(APPLICATION_SDP);
        try {
            multipart.part("session", objectMapper.writeValueAsString(buildSessionConfig(locale)))
                    .contentType(MediaType.APPLICATION_JSON);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_REALTIME_CONFIG_INVALID");
        }

        try {
            String answer = restClient.post()
                    .uri("/realtime/calls")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(APPLICATION_SDP)
                    .body(multipart.build())
                    .retrieve()
                    .body(String.class);
            if (answer == null || answer.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI_REALTIME_EMPTY_SDP");
            }
            return answer;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Échec création appel OpenAI Realtime status={}", exception.getStatusCode());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_UNAVAILABLE");
        } catch (RuntimeException exception) {
            log.warn("Échec création appel OpenAI Realtime", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_UNAVAILABLE");
        }
    }

    Map<String, Object> buildSessionConfig(String requestedLocale) {
        String locale = normalizeLocale(requestedLocale);
        Map<String, Object> transcription = new LinkedHashMap<>();
        transcription.put("model", transcriptionModel);
        transcription.put("language", locale);
        transcription.put("prompt", transcriptionPrompt(locale));

        Map<String, Object> turnDetection = new LinkedHashMap<>();
        turnDetection.put("type", "semantic_vad");
        turnDetection.put("eagerness", vadEagerness);
        turnDetection.put("create_response", false);
        turnDetection.put("interrupt_response", true);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("transcription", transcription);
        input.put("noise_reduction", Map.of("type", noiseReduction));
        input.put("turn_detection", turnDetection);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("voice", voice);
        output.put("speed", 1.0);

        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("input", input);
        audio.put("output", output);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("type", "realtime");
        session.put("model", model);
        session.put("instructions", realtimeInstructions(locale));
        session.put("audio", audio);
        session.put("tool_choice", "none");
        session.put("max_output_tokens", 450);
        return session;
    }

    private String realtimeInstructions(String locale) {
        if ("en".equals(locale)) {
            return "You are the real-time voice transport for Joprelys Clinical Copilot. "
                    + "Never diagnose, prescribe, alter medication, invent clinical facts, or make a clinical decision. "
                    + "Automatic answers are disabled. Only speak when the Joprelys client explicitly asks you to voice "
                    + "a backend-approved message. Reproduce that approved message faithfully, naturally and concisely, "
                    + "without adding medical content. Speak English unless the approved message is clearly in another language.";
        }
        return "Vous êtes la couche vocale temps réel de Joprelys Clinical Copilot. "
                + "Ne diagnostiquez jamais, ne prescrivez jamais, ne modifiez aucun médicament, n'inventez aucun fait clinique "
                + "et ne prenez aucune décision médicale. Les réponses automatiques sont désactivées. Parlez uniquement lorsque "
                + "le client Joprelys vous demande explicitement de vocaliser un message approuvé par le backend. Reproduisez ce "
                + "message fidèlement, naturellement et brièvement, sans ajouter de contenu médical. Parlez français sauf si le "
                + "message approuvé est clairement dans une autre langue.";
    }

    private String transcriptionPrompt(String locale) {
        if ("en".equals(locale)) {
            return "Medical consultation. Preserve negations, laterality, numbers, units, vital signs, drug names, dosages, "
                    + "routes, frequencies and durations exactly. Cameroon and international clinical vocabulary may occur.";
        }
        return "Consultation médicale. Conserver exactement les négations, la latéralité, les nombres, unités, constantes, "
                + "noms de médicaments, dosages, voies, fréquences et durées. Le vocabulaire clinique camerounais, francophone "
                + "et international peut être employé.";
    }

    private String normalizeLocale(String locale) {
        return locale != null && locale.toLowerCase().startsWith("en") ? "en" : "fr";
    }

    private String normalizeEagerness(String value) {
        if (value == null) {
            return "medium";
        }
        return switch (value.trim().toLowerCase()) {
            case "low", "medium", "high", "auto" -> value.trim().toLowerCase();
            default -> "medium";
        };
    }

    private String normalizeNoiseReduction(String value) {
        return "far_field".equalsIgnoreCase(value) ? "far_field" : "near_field";
    }
}
