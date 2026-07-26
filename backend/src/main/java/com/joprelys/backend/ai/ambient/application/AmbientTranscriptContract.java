package com.joprelys.backend.ai.ambient.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AmbientTranscriptContract {

    private AmbientTranscriptContract() {
    }

    public record TranscriptItemView(
            UUID id,
            long sequence,
            String sourceEventId,
            String source,
            String speakerType,
            String speakerLabel,
            String text,
            String locale,
            long startOffsetMs,
            long endOffsetMs,
            String status,
            UUID supersedesItemId,
            Instant createdAt) {
    }

    public record TranscriptLedgerView(
            UUID visitId,
            List<TranscriptItemView> items) {
    }
}
