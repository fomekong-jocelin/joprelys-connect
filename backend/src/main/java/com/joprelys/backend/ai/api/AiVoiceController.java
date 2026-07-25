package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiSpeechSynthesisService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class AiVoiceController {

    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm", "audio/mp4", "audio/mpeg", "audio/wav");

    private final OpenAiSpeechSynthesisService speechSynthesisService;
    private final AiProvider aiProvider;
    private final AiProperties properties;
    private final AiConsultationService consultationService;

    public AiVoiceController(
            OpenAiSpeechSynthesisService speechSynthesisService,
            AiProvider aiProvider,
            AiProperties properties,
            AiConsultationService consultationService) {
        this.speechSynthesisService = speechSynthesisService;
        this.aiProvider = aiProvider;
        this.properties = properties;
        this.consultationService = consultationService;
    }

    @PostMapping(
            value = "/api/ai/voice/speech",
            produces = "audio/mpeg")
    public ResponseEntity<byte[]> synthesizeSpeech(
            @Valid @RequestBody SpeechRequest request) {
        byte[] audio = speechSynthesisService.synthesize(request.text());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }

    @PostMapping(
            value = "/api/ai/consultations/{visitId}/clarifications/{clarificationId}/answer/audio",
            consumes = {"audio/webm", "audio/mp4", "audio/mpeg", "audio/wav"})
    public MessageView answerClarificationAudio(
            @PathVariable UUID visitId,
            @PathVariable UUID clarificationId,
            @RequestBody byte[] audio,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            Authentication authentication) {
        validateAudio(audio, contentType);
        Identity identity = identity(authentication);
        String normalizedMime = normalizeMimeType(contentType);
        AiTranscription transcription;
        try {
            transcription = aiProvider.transcribeAudio(audio, normalizedMime, properties.locale());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
        if (transcription == null
                || transcription.text() == null
                || transcription.text().isBlank()) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatusCode.valueOf(422), "AI_OUTPUT_INVALID");
        }
        return consultationService.answerClarification(
                visitId,
                identity.userId(),
                identity.organizationId(),
                clarificationId,
                transcription.text().trim());
    }

    private void validateAudio(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (audio.length > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_AUDIO_TOO_LARGE");
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizeMimeType(contentType))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
    }

    private String normalizeMimeType(String contentType) {
        return contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase();
    }

    private Identity identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        UUID userId = parseUuid(claims.subject())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "AUTH_IDENTITY_UNRESOLVED"));
        UUID organizationId = Optional.ofNullable(TenantContext.getTenantId())
                .or(() -> parseUuid(claims.organizationId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "AUTH_TENANT_UNRESOLVED"));
        return new Identity(userId, organizationId);
    }

    private Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record SpeechRequest(
            @NotBlank @Size(max = 4096) String text) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
