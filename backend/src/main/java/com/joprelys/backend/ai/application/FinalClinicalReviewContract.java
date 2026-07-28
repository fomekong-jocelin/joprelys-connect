package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class FinalClinicalReviewContract {

    private FinalClinicalReviewContract() {
    }

    public record ReviewView(
            UUID reviewId,
            UUID visitId,
            String status,
            String model,
            Instant createdAt,
            Instant expiresAt,
            RevisionView revision,
            Map<String, String> acceptedPatch) {
    }
}
