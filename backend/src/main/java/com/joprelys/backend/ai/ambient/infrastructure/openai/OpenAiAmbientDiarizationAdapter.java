package com.joprelys.backend.ai.ambient.infrastructure.openai;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class OpenAiAmbientDiarizationAdapter implements AmbientDiarizationPort {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAmbientDiarizationAdapter.class);
    private static final int MAX_AUDIO_BYTES = 25 * 1024 * 1024;

    private final RestClient restClient;
    private final String model;

    @Autowired
    public OpenAiAmbientDiarizationAdapter(
            AiProperties properties,
            @Value("${joprelys.ai.openai.ambient-transcribe-model:gpt-4o-transcribe-diarize}") String model) {
        this(buildRestClient(properties), model);
    }

    OpenAiAmbientDiarizationAdapter(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model == null || model.isBlank()
                ? "gpt-4o-transcribe-diarize"
                : model.trim();
    }

    private static RestClient buildRestClient(AiProperties properties) {
        AiProperties.OpenAiProperties openAi = properties == null ? null : properties.openai();
        return openAi == null
                || openAi.apiKey() == null
                || openAi.apiKey().isBlank()
                || openAi.baseUrl() == null
                || openAi.baseUrl().isBlank()
                ? null
                : RestClient.builder()
                        .baseUrl(openAi.baseUrl())
                        .defaultHeader("Authorization", "Bearer " + openAi.apiKey())
                        .build();
    }

    @Override
    public DiarizedTranscript transcribe(byte[] audio, String contentType, String locale) {
        validateAudio(audio, contentType);
        if (restClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_NOT_CONFIGURED");
        }
        if (!model.toLowerCase(Locale.ROOT).startsWith("gpt-4o-transcribe-diarize")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_DIARIZATION_MODEL_REQUIRED");
        }

        String extension = extension(contentType);
        ByteArrayResource resource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return "ambient." + extension;
            }
        };

        var formData = new LinkedMultiValueMap<String, Object>();
        formData.add("file", resource);
        formData.add("model", model);
        formData.add("response_format", "diarized_json");
        formData.add("chunking_strategy", "auto");
        String language = normalizeLanguage(locale);
        if (language != null) {
            formData.add("language", language);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(formData)
                    .retrieve()
                    .body(Map.class);
            return parse(response);
        } catch (RestClientResponseException exception) {
            log.warn("OpenAI ambient diarization rejected status={}", exception.getStatusCode());
            if (exception.getStatusCode().value() == 401 || exception.getStatusCode().value() == 403) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_AUTHENTICATION_FAILED");
            }
            if (exception.getStatusCode().value() == 402 || exception.getStatusCode().value() == 429) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_QUOTA_EXCEEDED");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_UPSTREAM_UNAVAILABLE");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("OpenAI ambient diarization failed", exception);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_AMBIENT_NETWORK_UNAVAILABLE");
        }
    }

    private DiarizedTranscript parse(Map<String, Object> response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI_AMBIENT_EMPTY_RESPONSE");
        }
        String text = response.get("text") instanceof String value ? value : "";
        Object rawSegments = response.get("segments");
        if (!(rawSegments instanceof List<?> segments)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI_AMBIENT_SEGMENTS_MISSING");
        }

        List<DiarizedSegment> parsed = new ArrayList<>();
        for (int index = 0; index < segments.size(); index++) {
            Object rawSegment = segments.get(index);
            if (!(rawSegment instanceof Map<?, ?> segment)) {
                continue;
            }
            String segmentText = asString(segment.get("text"));
            Number start = asNumber(segment.get("start"));
            Number end = asNumber(segment.get("end"));
            if (segmentText == null || segmentText.isBlank() || start == null || end == null) {
                continue;
            }
            parsed.add(new DiarizedSegment(
                    firstNonBlank(asString(segment.get("id")), Integer.toString(index)),
                    start.doubleValue(),
                    end.doubleValue(),
                    segmentText,
                    asString(segment.get("speaker"))));
        }
        return new DiarizedTranscript(text, List.copyOf(parsed));
    }

    private void validateAudio(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0 || audio.length > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_AUDIO_INVALID");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AMBIENT_AUDIO_TYPE_UNSUPPORTED");
        }
        extension(contentType);
    }

    private String extension(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("audio/webm")) return "webm";
        if (normalized.startsWith("audio/mp4") || normalized.startsWith("audio/m4a")) return "m4a";
        if (normalized.startsWith("audio/mpeg") || normalized.startsWith("audio/mp3")) return "mp3";
        if (normalized.startsWith("audio/wav")) return "wav";
        if (normalized.startsWith("audio/ogg")) return "ogg";
        if (normalized.startsWith("audio/flac")) return "flac";
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AMBIENT_AUDIO_TYPE_UNSUPPORTED");
    }

    private String normalizeLanguage(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        String language = normalized.split("-", 2)[0];
        return language.matches("[a-z]{2,3}") ? language : null;
    }

    private String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private Number asNumber(Object value) {
        return value instanceof Number number ? number : null;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
