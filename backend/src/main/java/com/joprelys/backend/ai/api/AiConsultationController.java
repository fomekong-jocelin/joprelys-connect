package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.application.AiConsultationContract.TranscriptionView;
import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class AiConsultationController {

    private final AiConsultationService service;

    public AiConsultationController(AiConsultationService service) {
        this.service = service;
    }

    @PostMapping("/{visitId}/sessions")
    public SessionView startSession(
            @PathVariable UUID visitId,
            @Valid @RequestBody(required = false) StartSessionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        Map<String, String> draft = request == null || request.draft() == null
                ? Map.of() : request.draft();
        if (request == null || request.locale() == null || request.locale().isBlank()) {
            return service.startSession(
                    visitId, identity.userId(), identity.organizationId(), draft);
        }
        return service.startSession(
                visitId,
                identity.userId(),
                identity.organizationId(),
                draft,
                request.locale());
    }

    @GetMapping("/{visitId}/session")
    public ResponseEntity<SessionView> getSession(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.getSession(visitId, identity.userId(), identity.organizationId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{visitId}/messages/text")
    public MessageView sendText(
            @PathVariable UUID visitId,
            @Valid @RequestBody TextMessageRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.processText(
                visitId, identity.userId(), identity.organizationId(), request.text());
    }

    @PostMapping("/{visitId}/clarifications/{clarificationId}/answer")
    public MessageView answerClarification(
            @PathVariable UUID visitId,
            @PathVariable UUID clarificationId,
            @Valid @RequestBody ClarificationAnswerRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.answerClarification(
                visitId,
                identity.userId(),
                identity.organizationId(),
                clarificationId,
                request.answer());
    }

    @PostMapping(
            "/{visitId}/revisions/{revisionId}/proposals/{proposalId}/decision")
    public SessionView decideProposal(
            @PathVariable UUID visitId,
            @PathVariable UUID revisionId,
            @PathVariable UUID proposalId,
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.decideProposal(
                visitId,
                identity.userId(),
                identity.organizationId(),
                revisionId,
                proposalId,
                request.decision());
    }

    @PostMapping("/{visitId}/revisions/{revisionId}/decision")
    public SessionView decideRevision(
            @PathVariable UUID visitId,
            @PathVariable UUID revisionId,
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.decideRevision(
                visitId,
                identity.userId(),
                identity.organizationId(),
                revisionId,
                request.decision());
    }

    @PostMapping(
            value = "/{visitId}/transcriptions/audio",
            consumes = {"audio/webm", "audio/mp4", "audio/mpeg", "audio/wav"})
    public TranscriptionView transcribeAudio(
            @PathVariable UUID visitId,
            @RequestBody byte[] audio,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            Authentication authentication) {
        requireContentType(contentType);
        Identity identity = identity(authentication);
        return service.transcribeAudio(
                visitId, identity.userId(), identity.organizationId(), audio, contentType);
    }

    @PostMapping("/{visitId}/transcriptions/analyze")
    public MessageView analyzeTranscript(
            @PathVariable UUID visitId,
            @Valid @RequestBody AnalyzeTranscriptRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.analyzeTranscript(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request.transcript());
    }

    @PostMapping("/{visitId}/transcriptions/realtime")
    public TranscriptionView stageRealtimeTranscript(
            @PathVariable UUID visitId,
            @Valid @RequestBody AnalyzeTranscriptRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return service.stageRealtimeTranscript(
                visitId,
                identity.userId(),
                identity.organizationId(),
                request.transcript());
    }

    @DeleteMapping("/{visitId}/transcriptions/pending")
    public ResponseEntity<Void> discardPendingTranscript(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        service.discardPendingTranscript(
                visitId, identity.userId(), identity.organizationId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{visitId}/messages/audio",
            consumes = {"audio/webm", "audio/mp4", "audio/mpeg", "audio/wav"})
    public MessageView sendAudio(
            @PathVariable UUID visitId,
            @RequestBody byte[] audio,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            Authentication authentication) {
        requireContentType(contentType);
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

    private void requireContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
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

    public record StartSessionRequest(
            Map<String, String> draft,
            @Pattern(regexp = "fr|en") String locale) {
    }

    public record TextMessageRequest(
            @NotBlank @Size(max = 12000) String text) {
    }

    public record ClarificationAnswerRequest(
            @NotBlank @Size(max = 12000) String answer) {
    }

    public record DecisionRequest(
            @NotBlank @Pattern(regexp = "ACCEPT|REJECT") String decision) {
    }

    public record AnalyzeTranscriptRequest(
            @NotBlank @Size(max = 12000) String transcript) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
