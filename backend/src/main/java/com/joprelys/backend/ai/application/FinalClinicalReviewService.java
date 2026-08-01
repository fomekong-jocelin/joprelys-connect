package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.ai.application.AiConsultationContract.FieldProposalView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import com.joprelys.backend.ai.application.FinalClinicalReviewContract.ReviewView;
import com.joprelys.backend.medication.reference.MedicationReferenceDuplicateDetector;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(
        name = "joprelys.ai.openai.final-review-enabled",
        havingValue = "true")
public class FinalClinicalReviewService {

    private static final long REVIEW_TTL_MINUTES = 30;
    private static final Set<String> REVIEW_FIELDS = Set.of(
            "symptoms",
            "clinicalExam",
            "diagnosis",
            "conclusion",
            "advice",
            "followUp",
            "prescription",
            "labOrders");

    private final FinalClinicalReviewGateway gateway;
    private final AiClinicalResponseParser responseParser;
    private final ClinicalContextAssembler clinicalContextAssembler;
    private final AiClinicalFactualityGuard factualityGuard;
    private final AiClinicalGroundingGuard groundingGuard;
    private final AiMedicationSafetyGuard medicationSafetyGuard;
    private final ConcurrentMap<ReviewKey, ReviewState> reviews = new ConcurrentHashMap<>();

    public FinalClinicalReviewService(
            FinalClinicalReviewGateway gateway,
            AiClinicalResponseParser responseParser,
            ClinicalContextAssembler clinicalContextAssembler,
            ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.responseParser = responseParser;
        this.clinicalContextAssembler = clinicalContextAssembler;
        this.factualityGuard = new AiClinicalFactualityGuard(objectMapper);
        this.groundingGuard = new AiClinicalGroundingGuard(objectMapper);
        this.medicationSafetyGuard = new AiMedicationSafetyGuard(objectMapper);
    }

    @Autowired(required = false)
    void setMedicationReferenceDuplicateDetector(
            MedicationReferenceDuplicateDetector referenceDuplicateDetector) {
        this.medicationSafetyGuard.setReferenceDuplicateDetector(referenceDuplicateDetector);
    }

    /** Backward-compatible narrative-only entry point used by existing callers/tests. */
    public ReviewView createReview(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> acceptedDraft,
            String locale) {
        return createReview(
                visitId,
                userId,
                organizationId,
                acceptedDraft,
                "",
                locale);
    }

    /**
     * Explicit deep reconciliation. The durable transcript is factual evidence only:
     * it enables Terra to surface an explicitly dictated omission but never grants
     * permission to infer a new diagnosis, medication, examination or numeric value.
     */
    public ReviewView createReview(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> acceptedDraft,
            String sourceTranscript,
            String locale) {
        if (visitId == null || userId == null || organizationId == null) {
            throw invalid("AI_FINAL_REVIEW_IDENTITY_INVALID");
        }
        Map<String, String> draft = sanitizeDraft(acceptedDraft);
        if (draft.isEmpty()) {
            throw invalid("AI_FINAL_REVIEW_DRAFT_EMPTY");
        }
        String transcript = sourceTranscript == null ? "" : sourceTranscript.trim();

        var response = gateway.review(draft, transcript, locale);
        ParsedResponse parsed = responseParser.parse(response.content());
        String factualSource = joinFactualSource(transcript, draft);
        ParsedResponse factChecked = factualityGuard.enforce(
                parsed, factualSource, draft, "FINAL_REVIEW", locale);
        ParsedResponse grounded = groundingGuard.enforce(
                factChecked, factualSource, null, locale);
        ParsedResponse medicationChecked = medicationSafetyGuard.enforce(
                grounded,
                clinicalContextAssembler.assemble(visitId),
                locale);

        RevisionView revision = buildRevision(draft, medicationChecked.changes());
        UUID reviewId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        ReviewState state = new ReviewState(
                reviewId,
                visitId,
                response.model(),
                createdAt,
                createdAt.plus(REVIEW_TTL_MINUTES, ChronoUnit.MINUTES),
                draft,
                revision);
        reviews.put(new ReviewKey(visitId, userId, organizationId), state);
        return toView(state);
    }

    public ReviewView decideProposal(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID reviewId,
            UUID proposalId,
            String decision,
            Map<String, String> currentDraft) {
        validateDecision(decision);
        ReviewState state = requireState(visitId, userId, organizationId, reviewId);
        synchronized (state) {
            requireFreshDraft(state, currentDraft);
            List<FieldProposalView> proposals = new ArrayList<>(state.revision.proposals());
            int proposalIndex = proposalIndex(proposals, proposalId);
            FieldProposalView proposal = proposals.get(proposalIndex);
            if (!"PENDING".equals(proposal.status())) {
                throw conflict("AI_FINAL_REVIEW_PROPOSAL_NOT_PENDING");
            }
            proposals.set(proposalIndex, decide(proposal, decision));
            state.revision = replaceRevision(state.revision, proposals);
            return toView(state);
        }
    }

    public ReviewView decideReview(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID reviewId,
            String decision,
            Map<String, String> currentDraft) {
        validateDecision(decision);
        ReviewState state = requireState(visitId, userId, organizationId, reviewId);
        synchronized (state) {
            requireFreshDraft(state, currentDraft);
            boolean hasPending = state.revision.proposals().stream()
                    .anyMatch(proposal -> "PENDING".equals(proposal.status()));
            if (!hasPending) {
                throw conflict("AI_FINAL_REVIEW_NOT_PENDING");
            }
            List<FieldProposalView> proposals = state.revision.proposals().stream()
                    .map(proposal -> "PENDING".equals(proposal.status())
                            ? decide(proposal, decision)
                            : proposal)
                    .toList();
            state.revision = replaceRevision(state.revision, proposals);
            return toView(state);
        }
    }

    private RevisionView buildRevision(
            Map<String, String> draft,
            List<ParsedChange> changes) {
        Map<String, ParsedChange> unique = new LinkedHashMap<>();
        for (ParsedChange change : changes) {
            if (change != null
                    && "SET".equals(change.operation())
                    && REVIEW_FIELDS.contains(change.field())) {
                unique.put(change.field(), change);
            }
        }
        Instant createdAt = Instant.now();
        List<FieldProposalView> proposals = new ArrayList<>();
        for (ParsedChange change : unique.values()) {
            String previous = draft.get(change.field());
            if (Objects.equals(previous, change.proposedValue())) {
                continue;
            }
            proposals.add(new FieldProposalView(
                    UUID.randomUUID(),
                    change.field(),
                    "SET",
                    previous,
                    change.proposedValue(),
                    change.reason(),
                    change.uncertainty(),
                    "PENDING",
                    createdAt,
                    null));
        }
        return new RevisionView(
                UUID.randomUUID(),
                1,
                proposals.isEmpty() ? "DECIDED" : "PENDING",
                createdAt,
                List.copyOf(proposals));
    }

    private String joinFactualSource(String transcript, Map<String, String> draft) {
        String draftSource = String.join("\n", draft.values());
        if (transcript == null || transcript.isBlank()) return draftSource;
        if (draftSource.isBlank()) return transcript;
        return transcript + "\n" + draftSource;
    }

    private ReviewState requireState(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID reviewId) {
        ReviewKey key = new ReviewKey(visitId, userId, organizationId);
        ReviewState state = reviews.get(key);
        if (state == null || !state.reviewId.equals(reviewId)) {
            throw conflict("AI_FINAL_REVIEW_NOT_FOUND");
        }
        if (state.expiresAt.isBefore(Instant.now())) {
            reviews.remove(key, state);
            throw conflict("AI_FINAL_REVIEW_EXPIRED");
        }
        return state;
    }

    private void requireFreshDraft(ReviewState state, Map<String, String> currentDraft) {
        if (!state.baseDraft.equals(sanitizeDraft(currentDraft))) {
            throw conflict("AI_FINAL_REVIEW_STALE");
        }
    }

    private ReviewView toView(ReviewState state) {
        Map<String, String> acceptedPatch = new LinkedHashMap<>();
        state.revision.proposals().stream()
                .filter(proposal -> "ACCEPTED".equals(proposal.status()))
                .filter(proposal -> proposal.proposedValue() != null)
                .forEach(proposal -> acceptedPatch.put(proposal.field(), proposal.proposedValue()));
        String status = state.revision.proposals().isEmpty()
                ? "NO_CHANGES"
                : "PENDING".equals(state.revision.status()) ? "PENDING" : "DECIDED";
        return new ReviewView(
                state.reviewId,
                state.visitId,
                status,
                state.model,
                state.createdAt,
                state.expiresAt,
                state.revision,
                Map.copyOf(acceptedPatch));
    }

    private RevisionView replaceRevision(
            RevisionView revision,
            List<FieldProposalView> proposals) {
        boolean pending = proposals.stream()
                .anyMatch(proposal -> "PENDING".equals(proposal.status()));
        return new RevisionView(
                revision.id(),
                revision.sequence(),
                pending ? "PENDING" : "DECIDED",
                revision.createdAt(),
                List.copyOf(proposals));
    }

    private FieldProposalView decide(FieldProposalView proposal, String decision) {
        return new FieldProposalView(
                proposal.id(),
                proposal.field(),
                proposal.operation(),
                proposal.previousValue(),
                proposal.proposedValue(),
                proposal.reason(),
                proposal.uncertainty(),
                "ACCEPT".equals(decision) ? "ACCEPTED" : "REJECTED",
                proposal.createdAt(),
                Instant.now());
    }

    private int proposalIndex(List<FieldProposalView> proposals, UUID proposalId) {
        for (int index = 0; index < proposals.size(); index++) {
            if (proposals.get(index).id().equals(proposalId)) {
                return index;
            }
        }
        throw conflict("AI_FINAL_REVIEW_PROPOSAL_NOT_FOUND");
    }

    private Map<String, String> sanitizeDraft(Map<String, String> draft) {
        if (draft == null || draft.isEmpty()) {
            return Map.of();
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        draft.forEach((field, value) -> {
            if (AiClinicalResponseParser.ALLOWED_FIELDS.contains(field)
                    && value != null
                    && !value.isBlank()) {
                sanitized.put(field, value.trim());
            }
        });
        return Map.copyOf(sanitized);
    }

    private void validateDecision(String decision) {
        if (!"ACCEPT".equals(decision) && !"REJECT".equals(decision)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_DECISION_INVALID");
        }
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }

    private record ReviewKey(UUID visitId, UUID userId, UUID organizationId) {
    }

    private static final class ReviewState {
        private final UUID reviewId;
        private final UUID visitId;
        private final String model;
        private final Instant createdAt;
        private final Instant expiresAt;
        private final Map<String, String> baseDraft;
        private RevisionView revision;

        private ReviewState(
                UUID reviewId,
                UUID visitId,
                String model,
                Instant createdAt,
                Instant expiresAt,
                Map<String, String> baseDraft,
                RevisionView revision) {
            this.reviewId = reviewId;
            this.visitId = visitId;
            this.model = model;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.baseDraft = baseDraft;
            this.revision = revision;
        }
    }
}
