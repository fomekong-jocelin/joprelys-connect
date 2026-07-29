package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiClinicalCaptureRebuildService;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/capture")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class AiClinicalCaptureController {

    private final AiClinicalCaptureRebuildService rebuildService;

    public AiClinicalCaptureController(AiClinicalCaptureRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    public SessionView rebuild(
            @PathVariable UUID visitId,
            @Valid @RequestBody(required = false) RebuildRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        Map<String, String> draft = request == null || request.draft() == null
                ? Map.of()
                : request.draft();
        String locale = request == null || request.locale() == null || request.locale().isBlank()
                ? "fr"
                : request.locale();
        return rebuildService.rebuild(
                visitId,
                identity.userId(),
                identity.organizationId(),
                draft,
                locale);
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
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record RebuildRequest(
            Map<String, String> draft,
            @Pattern(regexp = "fr|en") String locale) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
