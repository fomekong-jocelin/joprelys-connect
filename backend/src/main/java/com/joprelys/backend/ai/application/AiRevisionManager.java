package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiConsultationContract.FieldProposalView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
final class AiRevisionManager {

    private static final int MAX_REVISIONS = 10;
    private static final int MAX_EVIDENCE_ITEMS = 4;
    private static final Set<String> ADDITIVE_TEXT_FIELDS = Set.of(
            "symptoms",
            "clinicalExam",
            "suspectedDiagnosis",
            "conclusion",
            "advice",
            "followUp");

    private final AiClinicalMemoryManager memoryManager = new AiClinicalMemoryManager();

    RevisionView createRevision(
            AiConsultationSessionState state,
            List<ParsedChange> changes) {
        Map<String, List<ParsedChange>> grouped = new LinkedHashMap<>();
        changes.forEach(change -> grouped
                .computeIfAbsent(change.field(), ignored -> new ArrayList<>())
                .add(change));

        List<FieldProposalView> proposals = new ArrayList<>();
        Instant createdAt = Instant.now();
        grouped.values().forEach(group -> {
            ParsedChange change = consolidateChange(state, group);
            if (change == null) return;
            String previousValue = state.draft.get(change.field());
            if (isNoOp(previousValue, change)) {
                return;
            }
            proposals.add(new FieldProposalView(
                    UUID.randomUUID(),
                    change.field(),
                    change.operation(),
                    previousValue,
                    change.proposedValue(),
                    change.reason(),
                    change.uncertainty(),
                    "PENDING",
                    createdAt,
                    null));
        });
        if (proposals.isEmpty()) {
            return null;
        }
        RevisionView revision = new RevisionView(
                UUID.randomUUID(),
                state.nextRevisionSequence++,
                "PENDING",
                createdAt,
                List.copyOf(proposals));
        state.revisions.add(revision);
        trimHistory(state.revisions);
        return revision;
    }

    void decideProposal(
            AiConsultationSessionState state,
            UUID revisionId,
            UUID proposalId,
            String decision) {
        validateDecision(decision);
        int revisionIndex = revisionIndex(state.revisions, revisionId);
        RevisionView revision = state.revisions.get(revisionIndex);
        int proposalIndex = proposalIndex(revision.proposals(), proposalId);
        FieldProposalView proposal = revision.proposals().get(proposalIndex);
        if (!"PENDING".equals(proposal.status())) {
            throw conflict("AI_PROPOSAL_NOT_PENDING");
        }
        List<FieldProposalView> proposals = new ArrayList<>(revision.proposals());
        proposals.set(proposalIndex, decide(state, proposal, decision));
        state.revisions.set(revisionIndex, replaceRevision(revision, proposals));
        if ("ACCEPT".equals(decision)) {
            memoryManager.synchronizeAcceptedDraft(state);
        }
    }

    void decideRevision(
            AiConsultationSessionState state,
            UUID revisionId,
            String decision) {
        validateDecision(decision);
        int revisionIndex = revisionIndex(state.revisions, revisionId);
        RevisionView revision = state.revisions.get(revisionIndex);
        boolean hasPending = revision.proposals().stream()
                .anyMatch(proposal -> "PENDING".equals(proposal.status()));
        if (!hasPending) {
            throw conflict("AI_REVISION_NOT_PENDING");
        }
        List<FieldProposalView> proposals = revision.proposals().stream()
                .map(proposal -> "PENDING".equals(proposal.status())
                        ? decide(state, proposal, decision)
                        : proposal)
                .toList();
        state.revisions.set(revisionIndex, replaceRevision(revision, proposals));
        if ("ACCEPT".equals(decision)) {
            memoryManager.synchronizeAcceptedDraft(state);
        }
    }

    boolean hasPendingRevision(AiConsultationSessionState state) {
        return state.revisions.stream()
                .anyMatch(revision -> "PENDING".equals(revision.status()));
    }

    void ensureNoPendingRevision(AiConsultationSessionState state) {
        if (hasPendingRevision(state)) {
            throw conflict("AI_REVISION_DECISION_REQUIRED");
        }
    }

    private ParsedChange consolidateChange(
            AiConsultationSessionState state,
            List<ParsedChange> group) {
        if (group == null || group.isEmpty()) return null;
        ParsedChange first = group.getFirst();

        if (group.size() > 1) {
            boolean additiveSetGroup = ADDITIVE_TEXT_FIELDS.contains(first.field())
                    && group.stream().allMatch(item -> "SET".equals(item.operation()));
            if (!additiveSetGroup) {
                throw invalid("AI_CHANGE_DUPLICATE_FIELD");
            }

            String merged = "";
            LinkedHashSet<String> evidence = new LinkedHashSet<>();
            String uncertainty = "LOW";
            for (ParsedChange item : group) {
                merged = mergeNarrative(merged, item.proposedValue());
                if (item.evidence() != null) evidence.addAll(item.evidence());
                uncertainty = maxUncertainty(uncertainty, item.uncertainty());
            }
            first = new ParsedChange(
                    first.field(),
                    "SET",
                    merged,
                    first.reason(),
                    uncertainty,
                    evidence.stream().limit(MAX_EVIDENCE_ITEMS).toList());
        }

        if (!"SET".equals(first.operation()) || !ADDITIVE_TEXT_FIELDS.contains(first.field())) {
            return first;
        }

        String previous = state.draft.get(first.field());
        String mergedWithAccepted = mergeNarrative(previous, first.proposedValue());
        return new ParsedChange(
                first.field(),
                first.operation(),
                mergedWithAccepted,
                first.reason(),
                first.uncertainty(),
                first.evidence());
    }

    private String mergeNarrative(String left, String right) {
        String previous = left == null ? "" : left.trim();
        String proposed = right == null ? "" : right.trim();
        if (previous.isBlank()) return proposed;
        if (proposed.isBlank()) return previous;

        String normalizedPrevious = normalize(previous);
        String normalizedProposed = normalize(proposed);
        if (normalizedPrevious.contains(normalizedProposed)) return previous;
        if (normalizedProposed.contains(normalizedPrevious)) return proposed;

        String separator = endsWithSentencePunctuation(previous) ? " " : ". ";
        return previous + separator + proposed;
    }

    private boolean endsWithSentencePunctuation(String value) {
        return value.endsWith(".") || value.endsWith("!") || value.endsWith("?") || value.endsWith(":") || value.endsWith(";");
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String maxUncertainty(String left, String right) {
        return uncertaintyRank(right) > uncertaintyRank(left) ? right : left;
    }

    private int uncertaintyRank(String value) {
        return switch (value == null ? "" : value) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private FieldProposalView decide(
            AiConsultationSessionState state,
            FieldProposalView proposal,
            String decision) {
        if ("ACCEPT".equals(decision)) {
            if ("CLEAR".equals(proposal.operation())) {
                state.draft.remove(proposal.field());
            } else {
                state.draft.put(proposal.field(), proposal.proposedValue());
            }
        }
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

    private boolean isNoOp(String previousValue, ParsedChange change) {
        if ("CLEAR".equals(change.operation())) {
            return previousValue == null || previousValue.isBlank();
        }
        return Objects.equals(previousValue, change.proposedValue());
    }

    private int revisionIndex(List<RevisionView> revisions, UUID revisionId) {
        for (int index = 0; index < revisions.size(); index++) {
            if (revisions.get(index).id().equals(revisionId)) {
                return index;
            }
        }
        throw conflict("AI_REVISION_NOT_PENDING");
    }

    private int proposalIndex(List<FieldProposalView> proposals, UUID proposalId) {
        for (int index = 0; index < proposals.size(); index++) {
            if (proposals.get(index).id().equals(proposalId)) {
                return index;
            }
        }
        throw conflict("AI_PROPOSAL_NOT_PENDING");
    }

    private void validateDecision(String decision) {
        if (!"ACCEPT".equals(decision) && !"REJECT".equals(decision)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_DECISION_INVALID");
        }
    }

    private void trimHistory(List<RevisionView> revisions) {
        while (revisions.size() > MAX_REVISIONS) {
            int decidedIndex = firstDecidedIndex(revisions);
            if (decidedIndex < 0) {
                return;
            }
            revisions.remove(decidedIndex);
        }
    }

    private int firstDecidedIndex(List<RevisionView> revisions) {
        for (int index = 0; index < revisions.size(); index++) {
            if ("DECIDED".equals(revisions.get(index).status())) {
                return index;
            }
        }
        return -1;
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
