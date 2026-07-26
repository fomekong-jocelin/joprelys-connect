package com.joprelys.backend.ai.ambient.api;

import com.joprelys.backend.ai.ambient.application.AmbientNoteContract.NoteRevisionView;
import com.joprelys.backend.ai.ambient.application.AmbientNoteEngineService;
import com.joprelys.backend.ai.ambient.application.AmbientNotePersistenceService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/ai/consultations/{visitId}/ambient/notes")
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AmbientNoteController {

    private final AmbientNoteEngineService noteEngine;
    private final AmbientNotePersistenceService persistenceService;

    public AmbientNoteController(
            AmbientNoteEngineService noteEngine,
            AmbientNotePersistenceService persistenceService) {
        this.noteEngine = noteEngine;
        this.persistenceService = persistenceService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public NoteRevisionView generate(
            @PathVariable UUID visitId,
            @Valid @RequestBody(required = false) GenerateNoteRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return noteEngine.generate(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request == null ? "SOAP" : request.template(),
                request == null ? "fr" : request.locale());
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('CLINICAL_READ') or hasAuthority('CLINICAL_WRITE')")
    public ResponseEntity<NoteRevisionView> latest(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return persistenceService.latest(visitId, identity.organizationId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{noteId}/decision")
    @PreAuthorize("hasAuthority('CLINICAL_WRITE')")
    public NoteRevisionView decide(
            @PathVariable UUID visitId,
            @PathVariable UUID noteId,
            @Valid @RequestBody NoteDecisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return persistenceService.decide(
                visitId,
                noteId,
                identity.userId(),
                identity.organizationId(),
                request.decision());
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

    public record GenerateNoteRequest(
            @Pattern(regexp = "SOAP|APSO|MULTI_SECTION") String template,
            @Pattern(regexp = "[a-zA-Z]{2,3}(?:[-_][a-zA-Z]{2})?") String locale) {
    }

    public record NoteDecisionRequest(
            @Pattern(regexp = "ACCEPT|REJECT") String decision) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
