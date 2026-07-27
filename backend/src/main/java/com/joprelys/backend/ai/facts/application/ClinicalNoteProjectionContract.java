package com.joprelys.backend.ai.facts.application;

import java.util.List;
import java.util.UUID;

/**
 * Read-only projection contract built exclusively from effective, sourced clinical facts.
 *
 * <p>The projection is intentionally not a free-form LLM note. Every rendered entry keeps
 * direct links to the canonical FINAL transcript evidence that supports it. The backend
 * remains locale-neutral; the client renders labels from the central i18n catalog.</p>
 */
public final class ClinicalNoteProjectionContract {

    private ClinicalNoteProjectionContract() {
    }

    public enum NoteSectionCode {
        HISTORY_OF_PRESENT_ILLNESS,
        MEDICAL_HISTORY,
        ALLERGIES,
        VITALS,
        ASSESSMENT,
        MEDICATIONS,
        ORDERS,
        PLAN
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
            String conceptCode,
            String conceptText,
            String polarity,
            String valuePrimary,
            String valueSecondary,
            String unitCode,
            String temporalityText,
            String laterality,
            String frequencyText,
            String routeText,
            List<LinkedEvidenceView> evidence) {
    }

    public record NoteSectionView(
            NoteSectionCode code,
            List<NoteEntryView> entries) {
    }

    public record NoteProjectionView(
            UUID visitId,
            String projectionVersion,
            long maxFactSequence,
            List<NoteSectionView> sections) {
    }
}
