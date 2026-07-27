package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import java.util.List;
import java.util.UUID;

public final class ClinicalFactRevisionPlannerContract {

    private ClinicalFactRevisionPlannerContract() {
    }

    public record PlanRevisionRequest(
            String baseProjectionVersion,
            List<UUID> transcriptItemIds) {
    }

    public record RevisionPlanView(
            UUID planId,
            UUID visitId,
            String baseProjectionVersion,
            String model,
            List<UUID> transcriptItemIds,
            int modelOperationCount,
            int normalizedOperationCount,
            ApplyRevisionRequest proposedRevision) {
    }
}
