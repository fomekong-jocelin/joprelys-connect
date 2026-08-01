package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.ai.application.AiLiveTranscriptBufferService;
import com.joprelys.backend.ai.application.AiLiveTranscriptBufferService.Snapshot;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/transcriptions/live")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class AiLiveTranscriptController {

    private final AiConsultationService consultationService;
    private final AiLiveTranscriptBufferService bufferService;

    public AiLiveTranscriptController(
            AiConsultationService consultationService,
            AiLiveTranscriptBufferService bufferService) {
        this.consultationService = consultationService;
        this.bufferService = bufferService;
    }

    @PutMapping
    public LiveTranscriptView upsert(
            @PathVariable UUID visitId,
            @Valid @RequestBody LiveTranscriptRequest request,
            Authentication authentication) {
        Identity identity = requireSession(visitId, authentication);
        return view(bufferService.upsert(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request.transcript(),
                request.sequence(),
                request.eventId()));
    }

    @GetMapping
    public ResponseEntity<LiveTranscriptView> get(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = requireSession(visitId, authentication);
        return bufferService.get(
                        visitId,
                        identity.userId(),
                        identity.organizationId())
                .map(snapshot -> ResponseEntity.ok(view(snapshot)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = requireSession(visitId, authentication);
        bufferService.clear(
                visitId,
                identity.userId(),
                identity.organizationId());
        return ResponseEntity.noContent().build();
    }

    private Identity requireSession(
            UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        if (consultationService.getSession(
                visitId,
                identity.userId(),
                identity.organizationId()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_SESSION_EXPIRED");
        }
        return identity;
    }

    private LiveTranscriptView view(Snapshot snapshot) {
        return new LiveTranscriptView(
                snapshot.transcript(),
                snapshot.sequence(),
                snapshot.eventId(),
                snapshot.expiresAt());
    }

    private Identity identity(Authentication authentication) {
        if (authentication == null
                || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
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

    public record LiveTranscriptRequest(
            @NotBlank @Size(max = 32000) String transcript,
            @Min(0) long sequence,
            @Size(max = 200)
            @Pattern(regexp = "[A-Za-z0-9._:-]*") String eventId) {
    }

    public record LiveTranscriptView(
            String transcript,
            long sequence,
            String eventId,
            Instant expiresAt) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
