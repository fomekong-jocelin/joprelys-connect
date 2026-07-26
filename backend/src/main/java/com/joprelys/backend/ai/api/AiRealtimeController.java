package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiRealtimeCallService;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiRealtimeCallService.RealtimePurpose;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class AiRealtimeController {

    private static final MediaType APPLICATION_SDP = MediaType.valueOf("application/sdp");

    private final AiConsultationService consultationService;
    private final OpenAiRealtimeCallService realtimeCallService;

    public AiRealtimeController(
            AiConsultationService consultationService,
            OpenAiRealtimeCallService realtimeCallService) {
        this.consultationService = consultationService;
        this.realtimeCallService = realtimeCallService;
    }

    @PostMapping(
            value = "/api/ai/realtime/consultations/{visitId}/calls",
            consumes = "application/sdp",
            produces = "application/sdp")
    public ResponseEntity<String> createCall(
            @PathVariable UUID visitId,
            @RequestParam(defaultValue = "fr") String locale,
            @RequestBody String sdp,
            Authentication authentication) {
        Identity identity = identity(authentication);
        if (consultationService.getSession(
                visitId, identity.userId(), identity.organizationId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_SESSION_EXPIRED");
        }

        String answer = realtimeCallService.createCall(
                sdp,
                locale,
                RealtimePurpose.CONSULTATION);
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .contentType(APPLICATION_SDP)
                .body(answer);
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

    private record Identity(UUID userId, UUID organizationId) {
    }
}
