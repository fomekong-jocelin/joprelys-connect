package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RetractionReason;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEnvelope;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerEvidence;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerFact;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerOperation;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionPlannerModelContract.PlannerRetraction;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Authority;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactType;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Laterality;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.Polarity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class ClinicalFactRevisionPlanNormalizer {

    private static final int MAX_OPERATIONS = 64;
    private static final String DRY_RUN_EVENT_PREFIX = "planner-v1-";

    private final ClinicalFactEvidenceValidator evidenceValidator;
    private final ClinicalFactRetractionValidator retractionValidator;

    ClinicalFactRevisionPlanNormalizer(
            ClinicalFactEvidenceValidator evidenceValidator,
            ClinicalFactRetractionValidator retractionValidator) {
        this.evidenceValidator = evidenceValidator;
        this.retractionValidator = retractionValidator;
    }

    List<RevisionOperationRequest> normalize(
            UUID visitId,
            UUID organizationId,
            UUID revisionId,
            List<FactView> effectiveFacts,
            Map<UUID, TranscriptItemView> allowedTranscriptItems,
            PlannerEnvelope envelope) {
        if (envelope == null || envelope.operations() == null
                || envelope.operations().size() > MAX_OPERATIONS) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_OUTPUT_INVALID");
        }

        Map<UUID, FactView> effectiveById = new LinkedHashMap<>();
        for (FactView fact : effectiveFacts) {
            if (fact == null || fact.id() == null || effectiveById.put(fact.id(), fact) != null) {
                throw conflict("AI_CLINICAL_FACT_REVISION_PLAN_EFFECTIVE_FACTS_INVALID");
            }
        }

        List<RevisionOperationRequest> normalized = new ArrayList<>();
        List<FactPayload> plannedAdds = new ArrayList<>();
        Set<UUID> targetedFacts = new HashSet<>();

        for (PlannerOperation proposed : envelope.operations()) {
            RevisionOperationRequest operation = normalizeOperation(
                    visitId,
                    organizationId,
                    revisionId,
                    proposed,
                    effectiveById,
                    allowedTranscriptItems,
                    plannedAdds);
            if (operation.targetFactId() != null && !targetedFacts.add(operation.targetFactId())) {
                throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_TARGET_CONFLICT");
            }
            normalized.add(operation);
        }
        return List.copyOf(normalized);
    }

    private RevisionOperationRequest normalizeOperation(
            UUID visitId,
            UUID organizationId,
            UUID revisionId,
            PlannerOperation proposed,
            Map<UUID, FactView> effectiveById,
            Map<UUID, TranscriptItemView> allowedTranscriptItems,
            List<FactPayload> plannedAdds) {
        if (proposed == null || proposed.type() == null) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_OPERATION_INVALID");
        }
        OperationType type = enumValue(
                OperationType.class,
                proposed.type(),
                "AI_CLINICAL_FACT_REVISION_PLAN_OPERATION_TYPE_INVALID");
        UUID targetFactId = nullableUuid(
                proposed.targetFactId(),
                "AI_CLINICAL_FACT_REVISION_PLAN_TARGET_ID_INVALID");
        validateShape(type, targetFactId, proposed.fact(), proposed.retraction());

        UUID operationId = UUID.randomUUID();
        return switch (type) {
            case KEEP -> {
                requireEffectiveTarget(targetFactId, effectiveById);
                yield new RevisionOperationRequest(operationId, OperationType.KEEP, targetFactId, null, null);
            }
            case ADD -> normalizeAdd(
                    visitId,
                    organizationId,
                    revisionId,
                    operationId,
                    proposed.fact(),
                    effectiveById,
                    allowedTranscriptItems,
                    plannedAdds);
            case REPLACE -> normalizeReplace(
                    visitId,
                    organizationId,
                    revisionId,
                    operationId,
                    targetFactId,
                    proposed.fact(),
                    effectiveById,
                    allowedTranscriptItems);
            case RETRACT -> normalizeRetraction(
                    visitId,
                    organizationId,
                    operationId,
                    targetFactId,
                    proposed.retraction(),
                    effectiveById,
                    allowedTranscriptItems);
        };
    }

    private RevisionOperationRequest normalizeAdd(
            UUID visitId,
            UUID organizationId,
            UUID revisionId,
            UUID operationId,
            PlannerFact proposed,
            Map<UUID, FactView> effectiveById,
            Map<UUID, TranscriptItemView> allowedTranscriptItems,
            List<FactPayload> plannedAdds) {
        FactPayload payload = factPayload(proposed, allowedTranscriptItems);
        validatePatientPrimaryPolicy(payload, allowedTranscriptItems);
        dryRunFact(visitId, organizationId, revisionId, operationId, payload, null);

        List<FactView> duplicates = effectiveById.values().stream()
                .filter(current -> ClinicalFactRevisionSemanticGuard.sameClinicalMeaning(current, payload))
                .toList();
        if (duplicates.size() > 1) {
            throw conflict("AI_CLINICAL_FACT_REVISION_PLAN_DUPLICATE_EFFECTIVE_AMBIGUOUS");
        }
        if (duplicates.size() == 1) {
            return new RevisionOperationRequest(
                    operationId,
                    OperationType.KEEP,
                    duplicates.getFirst().id(),
                    null,
                    null);
        }
        if (plannedAdds.stream().anyMatch(
                existing -> ClinicalFactRevisionSemanticGuard.sameClinicalMeaning(existing, payload))) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_DUPLICATE_ADD");
        }
        plannedAdds.add(payload);
        return new RevisionOperationRequest(operationId, OperationType.ADD, null, payload, null);
    }

    private RevisionOperationRequest normalizeReplace(
            UUID visitId,
            UUID organizationId,
            UUID revisionId,
            UUID operationId,
            UUID targetFactId,
            PlannerFact proposed,
            Map<UUID, FactView> effectiveById,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        FactView target = requireEffectiveTarget(targetFactId, effectiveById);
        FactPayload payload = factPayload(proposed, allowedTranscriptItems);
        validateReplacementLineage(target, payload);
        validatePatientPrimaryPolicy(payload, allowedTranscriptItems);
        dryRunFact(visitId, organizationId, revisionId, operationId, payload, targetFactId);
        if (ClinicalFactRevisionSemanticGuard.sameClinicalMeaning(target, payload)) {
            return new RevisionOperationRequest(operationId, OperationType.KEEP, targetFactId, null, null);
        }
        return new RevisionOperationRequest(
                operationId,
                OperationType.REPLACE,
                targetFactId,
                payload,
                null);
    }

    private RevisionOperationRequest normalizeRetraction(
            UUID visitId,
            UUID organizationId,
            UUID operationId,
            UUID targetFactId,
            PlannerRetraction proposed,
            Map<UUID, FactView> effectiveById,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        FactView target = requireEffectiveTarget(targetFactId, effectiveById);
        RetractionPayload retraction = retractionPayload(proposed, allowedTranscriptItems);
        retractionValidator.validate(visitId, organizationId, target, retraction);
        return new RevisionOperationRequest(
                operationId,
                OperationType.RETRACT,
                targetFactId,
                null,
                retraction);
    }

    private void dryRunFact(
            UUID visitId,
            UUID organizationId,
            UUID revisionId,
            UUID operationId,
            FactPayload payload,
            UUID supersedesFactId) {
        FactCandidate candidate = new FactCandidate(
                DRY_RUN_EVENT_PREFIX + revisionId + "-" + operationId,
                payload.factType(),
                payload.authority(),
                payload.conceptCode(),
                payload.conceptText(),
                payload.polarity(),
                payload.valuePrimary(),
                payload.valueSecondary(),
                payload.unitCode(),
                payload.temporalityText(),
                payload.laterality(),
                payload.frequencyText(),
                payload.routeText(),
                FactStatus.ASSERTED,
                supersedesFactId,
                payload.evidence());
        evidenceValidator.validate(visitId, organizationId, candidate);
    }

    private FactPayload factPayload(
            PlannerFact proposed,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        if (proposed == null) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_FACT_REQUIRED");
        }
        return new FactPayload(
                enumValue(FactType.class, proposed.factType(), "AI_CLINICAL_FACT_REVISION_PLAN_FACT_TYPE_INVALID"),
                enumValue(Authority.class, proposed.authority(), "AI_CLINICAL_FACT_REVISION_PLAN_AUTHORITY_INVALID"),
                proposed.conceptCode(),
                proposed.conceptText(),
                enumValue(Polarity.class, proposed.polarity(), "AI_CLINICAL_FACT_REVISION_PLAN_POLARITY_INVALID"),
                blankToNull(proposed.valuePrimary()),
                blankToNull(proposed.valueSecondary()),
                blankToNull(proposed.unitCode()),
                blankToNull(proposed.temporalityText()),
                enumValue(Laterality.class, proposed.laterality(), "AI_CLINICAL_FACT_REVISION_PLAN_LATERALITY_INVALID"),
                blankToNull(proposed.frequencyText()),
                blankToNull(proposed.routeText()),
                evidence(proposed.evidence(), allowedTranscriptItems));
    }

    private RetractionPayload retractionPayload(
            PlannerRetraction proposed,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        if (proposed == null) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_RETRACTION_REQUIRED");
        }
        return new RetractionPayload(
                enumValue(Authority.class, proposed.authority(), "AI_CLINICAL_FACT_REVISION_PLAN_AUTHORITY_INVALID"),
                enumValue(RetractionReason.class, proposed.reason(), "AI_CLINICAL_FACT_REVISION_PLAN_RETRACTION_REASON_INVALID"),
                evidence(proposed.evidence(), allowedTranscriptItems));
    }

    private List<EvidenceSpanCandidate> evidence(
            List<PlannerEvidence> proposed,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        if (proposed == null || proposed.isEmpty() || proposed.size() > 8) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_EVIDENCE_INVALID");
        }
        List<EvidenceSpanCandidate> resolved = new ArrayList<>();
        for (PlannerEvidence item : proposed) {
            if (item == null || item.transcriptItemId() == null || item.quoteText() == null
                    || item.quoteText().isBlank() || item.quoteText().length() > 2_000) {
                throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_EVIDENCE_INVALID");
            }
            UUID transcriptItemId = parseUuid(
                    item.transcriptItemId(),
                    "AI_CLINICAL_FACT_REVISION_PLAN_EVIDENCE_ID_INVALID");
            TranscriptItemView transcriptItem = allowedTranscriptItems.get(transcriptItemId);
            if (transcriptItem == null) {
                throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_EVIDENCE_OUTSIDE_BATCH");
            }
            String text = transcriptItem.text();
            int start = text == null ? -1 : text.indexOf(item.quoteText());
            if (start < 0) {
                throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_QUOTE_NOT_EXACT");
            }
            if (text.indexOf(item.quoteText(), start + 1) >= 0) {
                throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_QUOTE_AMBIGUOUS");
            }
            resolved.add(new EvidenceSpanCandidate(
                    transcriptItemId,
                    start,
                    start + item.quoteText().length(),
                    item.quoteText(),
                    item.primarySupport()));
        }
        return List.copyOf(resolved);
    }

    private void validatePatientPrimaryPolicy(
            FactPayload payload,
            Map<UUID, TranscriptItemView> allowedTranscriptItems) {
        EvidenceSpanCandidate primary = payload.evidence().stream()
                .filter(EvidenceSpanCandidate::primarySupport)
                .findFirst()
                .orElse(null);
        if (primary == null) return;
        TranscriptItemView item = allowedTranscriptItems.get(primary.transcriptItemId());
        if (item == null || !"PATIENT".equals(item.speakerType())) return;
        if (payload.authority() != Authority.PATIENT_REPORTED) {
            throw clinicalInvalid("AI_CLINICAL_FACT_REVISION_PLAN_PATIENT_AUTHORITY_INVALID");
        }
        if (payload.factType() == FactType.ASSESSMENT
                || payload.factType() == FactType.PLAN
                || payload.factType() == FactType.ORDER) {
            throw clinicalInvalid("AI_CLINICAL_FACT_REVISION_PLAN_PATIENT_DECISION_INVALID");
        }
    }

    private void validateReplacementLineage(FactView target, FactPayload payload) {
        if (!target.factType().equals(payload.factType().name())
                || !target.authority().equals(payload.authority().name())
                || target.conceptCode() == null
                || payload.conceptCode() == null
                || !target.conceptCode().equals(payload.conceptCode().trim().toUpperCase(Locale.ROOT))) {
            throw clinicalInvalid("AI_CLINICAL_FACT_REVISION_PLAN_REPLACE_LINEAGE_INVALID");
        }
    }

    private FactView requireEffectiveTarget(UUID targetFactId, Map<UUID, FactView> effectiveById) {
        if (targetFactId == null) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_TARGET_REQUIRED");
        }
        FactView target = effectiveById.get(targetFactId);
        if (target == null) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_TARGET_NOT_EFFECTIVE");
        }
        return target;
    }

    private void validateShape(
            OperationType type,
            UUID targetFactId,
            PlannerFact fact,
            PlannerRetraction retraction) {
        boolean valid = switch (type) {
            case KEEP -> targetFactId != null && fact == null && retraction == null;
            case ADD -> targetFactId == null && fact != null && retraction == null;
            case REPLACE -> targetFactId != null && fact != null && retraction == null;
            case RETRACT -> targetFactId != null && fact == null && retraction != null;
        };
        if (!valid) {
            throw upstreamInvalid("AI_CLINICAL_FACT_REVISION_PLAN_OPERATION_SHAPE_INVALID");
        }
    }

    private UUID nullableUuid(String value, String errorCode) {
        if (value == null) return null;
        return parseUuid(value, errorCode);
    }

    private UUID parseUuid(String value, String errorCode) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorCode, exception);
        }
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, String errorCode) {
        if (value == null) {
            throw upstreamInvalid(errorCode);
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, errorCode, exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException upstreamInvalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, reason);
    }

    private ResponseStatusException clinicalInvalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
