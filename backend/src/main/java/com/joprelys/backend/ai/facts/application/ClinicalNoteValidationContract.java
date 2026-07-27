package com.joprelys.backend.ai.facts.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ClinicalNoteValidationContract {

    private ClinicalNoteValidationContract() {
    }

    public record ValidateProjectionRequest(
            @NotNull UUID validationId,
            @NotBlank @Size(max = 128) String projectionVersion) {
    }

    public record ValidatedFactRefView(
            UUID factId,
            long factSequence,
            String sectionCode,
            int position) {
    }

    public record NoteValidationView(
            UUID id,
            UUID validationId,
            UUID visitId,
            String projectionVersion,
            String projectionSchemaVersion,
            long maxFactSequence,
            UUID validatedByUserId,
            Instant validatedAt,
            List<ValidatedFactRefView> facts) {
    }

    public record NoteValidationHistoryView(
            UUID visitId,
            List<NoteValidationView> validations) {
    }
}
