package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN_CLINIQUE') and hasAuthority('CLINICAL_WRITE')")
public class AiConsultationController {

    private final AiConsultationService service;

    public AiConsultationController(AiConsultationService service) {
        this.service = service;
    }

    @PostMapping("/{visitId}/sessions")
    public AiConsultationService.SessionView startSession(
            @PathVariable UUID visitId,
            @Valid @RequestBody(required = false) StartSessionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        Map<String, String> draft = request == null || request.draft() == null
                ? Map.of() : request.draft();
        return service.startSession(
                visitId, identity.userId(), identity.organizationId(), draft);
    }

    @GetMapping("/{visitId}/session")
    public ResponseEntity<AiConsultationService.SessionView> getSession(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.getSession(visitId, identity.userId(), identity.organizationId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{visitId}/messages/text")
    public AiConsultationService.MessageView sendText(
            @PathVariable UUID visitId,
            @Valid @RequestBody TextMessageRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.processText(
                visitId, identity.userId(), identity.organizationId(), request.text());
    }

    @PostMapping(
            value = "/{visitId}/messages/audio",
            consumes = {"audio/webm", "audio/mp4", "audio/mpeg", "audio/wav"})
    public AiConsultationService.MessageView sendAudio(
            @PathVariable UUID visitId,
            @RequestBody byte[] audio,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            Authentication authentication) {
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
        Identity identity = identity(authentication);
        return service.processAudio(
                visitId, identity.userId(), identity.organizationId(), audio, contentType);
    }

    @DeleteMapping("/{visitId}/session")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        service.deleteSession(visitId, identity.userId(), identity.organizationId());
        return ResponseEntity.noContent().build();
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

    public record StartSessionRequest(Map<String, String> draft) {
    }

    public record TextMessageRequest(
            @NotBlank @Size(max = 12000) String text) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
