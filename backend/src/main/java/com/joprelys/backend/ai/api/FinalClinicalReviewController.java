package com.joprelys.backend.ai.api;

import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.ai.application.FinalClinicalReviewContract.ReviewView;
import com.joprelys.backend.ai.application.FinalClinicalReviewService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/ai/consultations")
@ConditionalOnProperty(
        name = "joprelys.ai.openai.final-review-enabled",
        havingValue = "true")
@PreAuthorize("hasAuthority('CLINICAL_WRITE')")
public class FinalClinicalReviewController {

    private final AiConsultationService consultationService;
    private final FinalClinicalReviewService reviewService;

    public FinalClinicalReviewController(
            AiConsultationService consultationService,
            FinalClinicalReviewService reviewService) {
        this.consultationService = consultationService;
        this.reviewService = reviewService;
    }

    @PostMapping("/{visitId}/final-review")
    public ReviewView createReview(
            @PathVariable UUID visitId,
            Authentication authentication) {
        Identity identity = identity(authentication);
        var session = consultationService.getSession(
                        visitId, identity.userId(), identity.organizationId())
                .orElseThrow(() -> conflict("AI_SESSION_EXPIRED"));
        boolean pendingRevision = session.revisions().stream()
                .anyMatch(revision -> "PENDING".equals(revision.status()));
        if (session.pendingTranscript() != null
                || session.needsClarification()
                || pendingRevision) {
            throw conflict("AI_FINAL_REVIEW_NOT_READY");
        }
        return reviewService.createReview(
                visitId,
                identity.userId(),
                identity.organizationId(),
                session.draft(),
                consultationService.sessionLocale(
                        visitId, identity.userId(), identity.organizationId()));
    }

    @PostMapping("/{visitId}/final-review/{reviewId}/proposals/{proposalId}/decision")
    public ReviewView decideProposal(
            @PathVariable UUID visitId,
            @PathVariable UUID reviewId,
            @PathVariable UUID proposalId,
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return reviewService.decideProposal(
                visitId,
                identity.userId(),
                identity.organizationId(),
                reviewId,
                proposalId,
                request.decision());
    }

    @PostMapping("/{visitId}/final-review/{reviewId}/decision")
    public ReviewView decideReview(
            @PathVariable UUID visitId,
            @PathVariable UUID reviewId,
            @Valid @RequestBody DecisionRequest request,
            Authentication authentication) {
        Identity identity = identity(authentication);
        return reviewService.decideReview(
                visitId,
                identity.userId(),
                identity.organizationId(),
                reviewId,
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
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    public record DecisionRequest(
            @Pattern(regexp = "ACCEPT|REJECT") String decision) {
    }

    private record Identity(UUID userId, UUID organizationId) {
    }
}
