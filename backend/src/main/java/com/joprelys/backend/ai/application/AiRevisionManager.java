package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiConsultationContract.FieldProposalView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
final class AiRevisionManager {

    private static final int MAX_REVISIONS = 10;

    RevisionView createRevision(
            AiConsultationSessionState state,
            List<ParsedChange> changes) {
        Map<String, ParsedChange> uniqueChanges = new LinkedHashMap<>();
        changes.forEach(change -> uniqueChanges.put(change.field(), change));
        List<FieldProposalView> proposals = new ArrayList<>();
        Instant createdAt = Instant.now();
        uniqueChanges.values().forEach(change -> {
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

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
