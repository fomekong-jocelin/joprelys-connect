package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiVitalsAssistantService;
import com.joprelys.backend.ai.application.AiVitalsAssistantService.VitalsAssistantResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@RestController
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiVitalsController {

    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");

    private final AiVitalsAssistantService service;
    private final ObjectMapper objectMapper;

    public AiVitalsController(AiVitalsAssistantService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/ai/vitals/{visitId}/text")
    @PreAuthorize("hasAuthority('VISIT_VITALS_WRITE')")
    public VitalsAssistantResponse analyzeText(
            @PathVariable UUID visitId,
            @Valid @RequestBody VitalsTextRequest request) {
        return service.analyzeText(
                visitId,
                request.text(),
                request.locale(),
                normalizeCurrentVitals(request.currentVitals()));
    }

    @PostMapping(
            value = "/api/ai/vitals/{visitId}/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VISIT_VITALS_WRITE')")
    public VitalsAssistantResponse analyzeAudio(
            @PathVariable UUID visitId,
            @RequestPart("audio") MultipartFile audio,
            @RequestPart(value = "locale", required = false) String locale,
            @RequestPart(value = "currentVitals", required = false) String currentVitals) {
        validateAudio(audio);
        return service.analyzeAudio(
                visitId,
                readBytes(audio),
                normalizeMimeType(audio.getContentType()),
                locale,
                parseCurrentVitals(currentVitals));
    }

    private byte[] readBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
    }

    private void validateAudio(MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (audio.getSize() > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_AUDIO_TOO_LARGE");
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizeMimeType(audio.getContentType()))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> parseCurrentVitals(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return normalizeCurrentVitals(objectMapper.readValue(json, Map.class));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
    }

    private Map<String, Double> normalizeCurrentVitals(Map<String, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (value instanceof Number number) {
                double numeric = number.doubleValue();
                if (Double.isFinite(numeric)) {
                    result.put(key, numeric);
                }
            }
        });
        return result;
    }

    private String normalizeMimeType(String contentType) {
        return contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase();
    }

    public record VitalsTextRequest(
            @NotBlank @Size(max = 3000) String text,
            @Size(max = 5) String locale,
            Map<String, ?> currentVitals) {
    }
}
