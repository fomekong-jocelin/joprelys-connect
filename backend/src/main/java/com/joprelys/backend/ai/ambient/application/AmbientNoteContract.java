package com.joprelys.backend.ai.ambient.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AmbientNoteContract {

    private AmbientNoteContract() {
    }

    public record NoteStatementView(
            UUID id,
            String section,
            int order,
            String text,
            boolean critical,
            List<UUID> evidenceItemIds) {
    }

    public record NoteRevisionView(
            UUID id,
            UUID visitId,
            long revision,
            String template,
            String locale,
            String status,
            long transcriptMaxSequence,
            String model,
            Integer tokensUsed,
            UUID supersedesNoteId,
            Instant createdAt,
            Instant decidedAt,
            UUID decidedByUserId,
            List<NoteStatementView> statements) {
    }
}
