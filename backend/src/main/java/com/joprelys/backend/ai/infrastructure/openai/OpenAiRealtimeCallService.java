package com.joprelys.backend.ai.infrastructure.openai;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final String fallbackModel;
    private final String voice;
    private final String transcriptionModel;
    private final String vadEagerness;
    private final String noiseReduction;

    public OpenAiRealtimeCallService(
            AiProperties properties,
            ObjectMapper objectMapper,
            @Value("${joprelys.ai.openai.realtime-model:gpt-realtime}") String model,
            @Value("${joprelys.ai.openai.realtime-fallback-model:gpt-realtime-mini}") String fallbackModel,
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
        this.model = normalizeModel(model, "gpt-realtime");
        this.fallbackModel = normalizeModel(fallbackModel, "gpt-realtime-mini");
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

        List<CallAttempt> attempts = callAttempts(locale);
        RestClientResponseException lastUpstreamError = null;
        for (int index = 0; index < attempts.size(); index++) {
            CallAttempt attempt = attempts.get(index);
            try {
                String answer = postCall(sdpOffer, attempt.session());
                log.info(
                        "OpenAI Realtime call created model={} compatibilityMode={}",
                        attempt.model(),
                        attempt.compatibilityMode());
                return answer;
            } catch (RestClientResponseException exception) {
                lastUpstreamError = exception;
                logUpstreamFailure(exception, attempt);
                if (!isRetryableConfigurationFailure(exception) || index == attempts.size() - 1) {
                    throw mapUpstreamFailure(exception);
                }
            } catch (ResponseStatusException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.warn("Échec création appel OpenAI Realtime", exception);
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_NETWORK_UNAVAILABLE");
            }
        }

        if (lastUpstreamError != null) {
            throw mapUpstreamFailure(lastUpstreamError);
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_UNAVAILABLE");
    }

    private String postCall(String sdpOffer, Map<String, Object> sessionConfig) {
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("sdp", sdpOffer.trim()).contentType(APPLICATION_SDP);
        try {
            multipart.part("session", objectMapper.writeValueAsString(sessionConfig))
                    .contentType(MediaType.APPLICATION_JSON);
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "AI_REALTIME_CONFIG_INVALID");
        }

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
    }

    private List<CallAttempt> callAttempts(String locale) {
        List<CallAttempt> attempts = new ArrayList<>();
        attempts.add(new CallAttempt(model, false, buildSessionConfig(locale, model)));
        attempts.add(new CallAttempt(model, true, buildCompatibilitySessionConfig(locale, model)));
        if (!fallbackModel.equals(model)) {
            attempts.add(new CallAttempt(
                    fallbackModel,
                    true,
                    buildCompatibilitySessionConfig(locale, fallbackModel)));
        }
        return List.copyOf(attempts);
    }

    Map<String, Object> buildSessionConfig(String requestedLocale) {
        return buildSessionConfig(requestedLocale, model);
    }

    private Map<String, Object> buildSessionConfig(String requestedLocale, String selectedModel) {
        String locale = normalizeLocale(requestedLocale);
        Map<String, Object> transcription = transcription(locale);

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

        Map<String, Object> session = baseSession(locale, selectedModel);
        session.put("audio", audio);
        session.put("tool_choice", "none");
        session.put("max_output_tokens", 450);
        return session;
    }

    private Map<String, Object> buildCompatibilitySessionConfig(
            String requestedLocale,
            String selectedModel) {
        String locale = normalizeLocale(requestedLocale);
        Map<String, Object> turnDetection = new LinkedHashMap<>();
        turnDetection.put("type", "server_vad");
        turnDetection.put("create_response", false);
        turnDetection.put("interrupt_response", true);
        turnDetection.put("silence_duration_ms", 650);
        turnDetection.put("prefix_padding_ms", 300);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("transcription", transcription(locale));
        input.put("turn_detection", turnDetection);

        Map<String, Object> audio = new LinkedHashMap<>();
        audio.put("input", input);
        audio.put("output", Map.of("voice", voice));

        Map<String, Object> session = baseSession(locale, selectedModel);
        session.put("audio", audio);
        return session;
    }

    private Map<String, Object> baseSession(String locale, String selectedModel) {
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("type", "realtime");
        session.put("model", selectedModel);
        session.put("output_modalities", List.of("audio"));
        session.put("instructions", realtimeInstructions(locale));
        return session;
    }

    private Map<String, Object> transcription(String locale) {
        Map<String, Object> transcription = new LinkedHashMap<>();
        transcription.put("model", transcriptionModel);
        transcription.put("language", locale);
        transcription.put("prompt", transcriptionPrompt(locale));
        return transcription;
    }

    private boolean isRetryableConfigurationFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 400 || status == 404 || status == 409 || status == 422;
    }

    private ResponseStatusException mapUpstreamFailure(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_AUTHENTICATION_FAILED");
        }
        if (status == 403) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_ACCESS_DENIED");
        }
        if (status == 402 || status == 429) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_QUOTA_EXCEEDED");
        }
        if (status == 400 || status == 404 || status == 422) {
            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_MODEL_OR_CONFIG_UNAVAILABLE");
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "AI_REALTIME_UPSTREAM_UNAVAILABLE");
    }

    private void logUpstreamFailure(
            RestClientResponseException exception,
            CallAttempt attempt) {
        String body = exception.getResponseBodyAsString();
        String safeBody = body == null ? "" : body.replaceAll("[\\r\\n]+", " ");
        if (safeBody.length() > 600) {
            safeBody = safeBody.substring(0, 600);
        }
        log.warn(
                "OpenAI Realtime rejected call status={} model={} compatibilityMode={} response={}",
                exception.getStatusCode(),
                attempt.model(),
                attempt.compatibilityMode(),
                safeBody);
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

    private String normalizeModel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private record CallAttempt(
            String model,
            boolean compatibilityMode,
            Map<String, Object> session) {
    }
}
