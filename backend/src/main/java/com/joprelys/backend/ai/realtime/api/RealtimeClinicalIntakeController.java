package com.joprelys.backend.ai.realtime.api;

import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/consultations/{visitId}/realtime-intake")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class RealtimeClinicalIntakeController {

    private final RealtimeClinicalIntakeService service;

    public RealtimeClinicalIntakeController(RealtimeClinicalIntakeService service) {
        this.service = service;
    }

    @PostMapping
    public IntakeView ingest(
            @PathVariable UUID visitId,
            @Valid @RequestBody IntakeRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.ingest(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request.eventId(),
                request.itemId(),
                request.transcript(),
                request.confidence());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public List<IntakeView> list(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.list(visitId, identity.organizationId());
    }

    private Identity identity(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtClaims claims)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
        }
        UUID userId = parseUuid(claims.subject())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_IDENTITY_UNRESOLVED"));
        UUID organizationId = Optional.ofNullable(TenantContext.getTenantId())
                .or(() -> parseUuid(claims.organizationId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_TENANT_UNRESOLVED"));
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

    public record IntakeRequest(
            @NotBlank @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9._:-]+") String eventId,
            @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9._:-]+") String itemId,
            @NotBlank @Size(max = 12000) String transcript,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double confidence) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
