package com.joprelys.backend.ai.facts.application;

import java.util.List;
import java.util.UUID;

/**
 * Read-only projection contract built exclusively from effective, sourced clinical facts.
 *
 * <p>The projection is intentionally not a free-form LLM note. Every rendered entry keeps
 * direct links to the canonical FINAL transcript evidence that supports it.</p>
 */
public final class ClinicalNoteProjectionContract {

    private ClinicalNoteProjectionContract() {
    }

    public record LinkedEvidenceView(
            UUID transcriptItemId,
            String speakerType,
            String speakerLabel,
            long startOffsetMs,
            long endOffsetMs,
            int quoteStartChar,
            int quoteEndChar,
            String quoteText,
            boolean primarySupport) {
    }

    public record NoteEntryView(
            UUID factId,
            long factSequence,
            String factType,
            String authority,
            String polarity,
            String text,
            List<LinkedEvidenceView> evidence) {
    }

    public record NoteSectionView(
            String code,
            List<NoteEntryView> entries) {
    }

    public record NoteProjectionView(
            UUID visitId,
            String projectionVersion,
            long maxFactSequence,
            List<NoteSectionView> sections) {
    }
}
