package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalFactContract.EvidenceSpanCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactCandidate;
import com.joprelys.backend.ai.facts.application.ClinicalFactContract.FactView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.ApplyRevisionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.FactPayload;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.OperationType;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionBatchView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionHistoryView;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationRequest;
import com.joprelys.backend.ai.facts.application.ClinicalFactRevisionContract.RevisionOperationView;
import com.joprelys.backend.ai.facts.domain.ClinicalFactTypes.FactStatus;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionBatchEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionBatchRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionEvidenceRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalFactRevisionOperationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class ClinicalFactRevisionService {

    private static final int MAX_OPERATIONS = 100;
    private static final String SOURCE_EVENT_PREFIX = "revision-v1-";

    private final ClinicalNoteProjectionService projectionService;
    private final ClinicalFactLedgerService factLedgerService;
    private final ClinicalFactRetractionValidator retractionValidator;
    private final ClinicalFactRevisionRequestHasher requestHasher;
    private final ClinicalFactRevisionBatchRepository batchRepository;
    private final ClinicalFactRevisionOperationRepository operationRepository;
    private final ClinicalFactRevisionEvidenceRepository revisionEvidenceRepository;
    private final ClinicalFactRepository factRepository;
    private final VisitRepository visitRepository;

    public ClinicalFactRevisionService(
            ClinicalNoteProjectionService projectionService,
            ClinicalFactLedgerService factLedgerService,
            ClinicalFactRetractionValidator retractionValidator,
            ClinicalFactRevisionRequestHasher requestHasher,
            ClinicalFactRevisionBatchRepository batchRepository,
            ClinicalFactRevisionOperationRepository operationRepository,
            ClinicalFactRevisionEvidenceRepository revisionEvidenceRepository,
            ClinicalFactRepository factRepository,
            VisitRepository visitRepository) {
        this.projectionService = projectionService;
        this.factLedgerService = factLedgerService;
        this.retractionValidator = retractionValidator;
        this.requestHasher = requestHasher;
        this.batchRepository = batchRepository;
        this.operationRepository = operationRepository;
        this.revisionEvidenceRepository = revisionEvidenceRepository;
        this.factRepository = factRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public RevisionBatchView apply(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            ApplyRevisionRequest request) {
        validateRequestEnvelope(visitId, userId, organizationId, request);
        String requestHash = requestHasher.sha256(request);
        lockAuthorizedVisit(visitId, organizationId);

        var existing = batchRepository.findByVisitIdAndRevisionRequestId(visitId, request.revisionId());
        if (existing.isPresent()) {
            return validateIdempotentRetry(existing.get(), userId, requestHash);
        }

        var currentProjection = projectionService.project(visitId, organizationId);
        String requestedBaseVersion = request.baseProjectionVersion().trim();
        if (!currentProjection.projectionVersion().equals(requestedBaseVersion)) {
            throw conflict("AI_CLINICAL_FACT_REVISION_STALE");
        }

        Map<UUID, FactView> effectiveFacts = new HashMap<>();
        factLedgerService.listEffective(visitId, organizationId).facts()
                .forEach(fact -> effectiveFacts.put(fact.id(), fact));
        validateOperations(visitId, request.operations(), effectiveFacts);

        ClinicalFactRevisionBatchEntity batch = new ClinicalFactRevisionBatchEntity(
                organizationId,
                visitId,
                request.revisionId(),
                requestedBaseVersion,
                requestHash,
                userId);
        batchRepository.save(batch);

        try {
            for (int position = 0; position < request.operations().size(); position++) {
                executeOperation(
                        batch,
                        position,
                        visitId,
                        userId,
                        organizationId,
                        request.operations().get(position),
                        effectiveFacts);
            }

            var resultingProjection = projectionService.project(visitId, organizationId);
            batch.complete(resultingProjection.projectionVersion());
            batchRepository.saveAndFlush(batch);
            return view(batch);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_CLINICAL_FACT_REVISION_CONCURRENT_CONFLICT",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public RevisionHistoryView history(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        return new RevisionHistoryView(
                visitId,
                batchRepository.findByVisitIdOrderByCreatedAtDesc(visitId)
                        .stream()
                        .map(this::view)
                        .toList());
    }

    private void executeOperation(
            ClinicalFactRevisionBatchEntity batch,
            int position,
            UUID visitId,
            UUID userId,
            UUID organizationId,
            RevisionOperationRequest operation,
            Map<UUID, FactView> effectiveFacts) {
        switch (operation.type()) {
            case KEEP -> persistOperation(batch, position, operation, operation.targetFactId(), null, null);
            case ADD -> {
                FactView result = appendFact(
                        visitId,
                        userId,
                        organizationId,
                        batch.getRevisionRequestId(),
                        operation,
                        null);
                persistOperation(batch, position, operation, null, result.id(), null);
            }
            case REPLACE -> {
                FactView result = appendFact(
                        visitId,
                        userId,
                        organizationId,
                        batch.getRevisionRequestId(),
                        operation,
                        operation.targetFactId());
                persistOperation(batch, position, operation, operation.targetFactId(), result.id(), null);
            }
            case RETRACT -> {
                FactView target = effectiveFacts.get(operation.targetFactId());
                List<EvidenceSpanCandidate> evidence = retractionValidator.validate(
                        visitId,
                        organizationId,
                        target,
                        operation.retraction());
                ClinicalFactRevisionOperationEntity persisted = persistOperation(
                        batch,
                        position,
                        operation,
                        operation.targetFactId(),
                        null,
                        operation.retraction().reason());
                revisionEvidenceRepository.saveAll(evidence.stream()
                        .map(span -> new ClinicalFactRevisionEvidenceEntity(
                                persisted.getId(),
                                span.transcriptItemId(),
                                span.quoteStartChar(),
                                span.quoteEndChar(),
                                span.quoteText(),
                                span.primarySupport()))
                        .toList());
            }
        }
    }

    private FactView appendFact(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            UUID revisionId,
            RevisionOperationRequest operation,
            UUID supersedesFactId) {
        FactPayload payload = operation.fact();
        FactCandidate candidate = new FactCandidate(
                sourceEventId(revisionId, operation.operationId()),
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
        return factLedgerService.appendValidatedFact(
                visitId,
                userId,
                organizationId,
                candidate);
    }

    private ClinicalFactRevisionOperationEntity persistOperation(
            ClinicalFactRevisionBatchEntity batch,
            int position,
            RevisionOperationRequest request,
            UUID targetFactId,
            UUID resultFactId,
            ClinicalFactRevisionContract.RetractionReason retractionReason) {
        return operationRepository.save(new ClinicalFactRevisionOperationEntity(
                batch.getId(),
                batch.getOrganizationId(),
                batch.getVisitId(),
                request.operationId(),
                position,
                request.type(),
                targetFactId,
                resultFactId,
                retractionReason));
    }

    private void validateOperations(
            UUID visitId,
            List<RevisionOperationRequest> operations,
            Map<UUID, FactView> effectiveFacts) {
        Set<UUID> operationIds = new HashSet<>();
        Set<UUID> targetedFacts = new HashSet<>();

        for (RevisionOperationRequest operation : operations) {
            if (operation == null || operation.operationId() == null || operation.type() == null) {
                throw invalid("AI_CLINICAL_FACT_REVISION_OPERATION_INVALID");
            }
            if (!operationIds.add(operation.operationId())) {
                throw invalid("AI_CLINICAL_FACT_REVISION_DUPLICATE_OPERATION_ID");
            }
            if (operationRepository.findByVisitIdAndOperationRequestId(visitId, operation.operationId()).isPresent()) {
                throw conflict("AI_CLINICAL_FACT_REVISION_OPERATION_ID_REUSED");
            }

            validateOperationShape(operation);
            if (operation.targetFactId() != null) {
                if (!targetedFacts.add(operation.targetFactId())) {
                    throw conflict("AI_CLINICAL_FACT_REVISION_TARGET_CONFLICT");
                }
                if (!effectiveFacts.containsKey(operation.targetFactId())) {
                    throw conflict("AI_CLINICAL_FACT_REVISION_TARGET_NOT_EFFECTIVE");
                }
            }
        }
    }

    private void validateOperationShape(RevisionOperationRequest operation) {
        boolean hasTarget = operation.targetFactId() != null;
        boolean hasFact = operation.fact() != null;
        boolean hasRetraction = operation.retraction() != null;

        boolean valid = switch (operation.type()) {
            case KEEP -> hasTarget && !hasFact && !hasRetraction;
            case ADD -> !hasTarget && hasFact && !hasRetraction;
            case REPLACE -> hasTarget && hasFact && !hasRetraction;
            case RETRACT -> hasTarget && !hasFact && hasRetraction;
        };
        if (!valid) {
            throw invalid("AI_CLINICAL_FACT_REVISION_OPERATION_SHAPE_INVALID");
        }
    }

    private void validateRequestEnvelope(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            ApplyRevisionRequest request) {
        if (visitId == null || userId == null || organizationId == null || request == null
                || request.revisionId() == null
                || request.baseProjectionVersion() == null
                || request.baseProjectionVersion().isBlank()
                || request.baseProjectionVersion().length() > 128
                || request.operations() == null
                || request.operations().isEmpty()
                || request.operations().size() > MAX_OPERATIONS) {
            throw invalid("AI_CLINICAL_FACT_REVISION_INVALID");
        }
    }

    private RevisionBatchView validateIdempotentRetry(
            ClinicalFactRevisionBatchEntity existing,
            UUID userId,
            String requestHash) {
        if (!existing.getCreatedByUserId().equals(userId)
                || !existing.getRequestSha256().equals(requestHash)) {
            throw conflict("AI_CLINICAL_FACT_REVISION_ID_REUSED");
        }
        return view(existing);
    }

    private RevisionBatchView view(ClinicalFactRevisionBatchEntity batch) {
        List<RevisionOperationView> operations = operationRepository
                .findByBatchIdOrderByPositionNoAsc(batch.getId())
                .stream()
                .map(this::operationView)
                .toList();
        return new RevisionBatchView(
                batch.getId(),
                batch.getRevisionRequestId(),
                batch.getVisitId(),
                batch.getBaseProjectionVersion(),
                batch.getResultProjectionVersion(),
                batch.getCreatedByUserId(),
                batch.getCreatedAt(),
                operations);
    }

    private RevisionOperationView operationView(ClinicalFactRevisionOperationEntity operation) {
        List<EvidenceSpanCandidate> evidence;
        if (operation.getOperationType() == OperationType.RETRACT) {
            evidence = revisionEvidenceRepository
                    .findByOperationIdOrderByPrimarySupportDescQuoteStartCharAsc(operation.getId())
                    .stream()
                    .map(this::evidenceView)
                    .toList();
        } else if (operation.getResultFactId() != null) {
            ClinicalFactEntity fact = factRepository
                    .findByIdAndVisitId(operation.getResultFactId(), operation.getVisitId())
                    .orElseThrow(() -> conflict("AI_CLINICAL_FACT_REVISION_AUDIT_BROKEN"));
            evidence = fact.getEvidence().stream()
                    .map(item -> new EvidenceSpanCandidate(
                            item.getTranscriptItemId(),
                            item.getQuoteStartChar(),
                            item.getQuoteEndChar(),
                            item.getQuoteText(),
                            item.isPrimarySupport()))
                    .toList();
        } else {
            evidence = List.of();
        }
        return new RevisionOperationView(
                operation.getOperationRequestId(),
                operation.getPositionNo(),
                operation.getOperationType().name(),
                operation.getTargetFactId(),
                operation.getResultFactId(),
                operation.getRetractionReason() == null ? null : operation.getRetractionReason().name(),
                evidence);
    }

    private EvidenceSpanCandidate evidenceView(ClinicalFactRevisionEvidenceEntity evidence) {
        return new EvidenceSpanCandidate(
                evidence.getTranscriptItemId(),
                evidence.getQuoteStartChar(),
                evidence.getQuoteEndChar(),
                evidence.getQuoteText(),
                evidence.isPrimarySupport());
    }

    private String sourceEventId(UUID revisionId, UUID operationId) {
        return SOURCE_EVENT_PREFIX + revisionId + "-" + operationId;
    }

    private VisitEntity lockAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        return visit;
    }

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        if (visitId == null || organizationId == null) {
            throw invalid("AI_CLINICAL_FACT_REVISION_IDENTITY_INVALID");
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, reason);
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
