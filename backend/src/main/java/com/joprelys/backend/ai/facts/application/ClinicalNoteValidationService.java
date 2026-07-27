package com.joprelys.backend.ai.facts.application;

import com.joprelys.backend.ai.facts.application.ClinicalNoteProjectionContract.NoteProjectionView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.NoteValidationHistoryView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.NoteValidationView;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.ValidateProjectionRequest;
import com.joprelys.backend.ai.facts.application.ClinicalNoteValidationContract.ValidatedFactRefView;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationFactEntity;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationFactRepository;
import com.joprelys.backend.ai.facts.infrastructure.persistence.ClinicalNoteValidationRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Persists explicit physician validation of one deterministic note projection.
 *
 * <p>The visit row is pessimistically locked before recomputing the projection. The client
 * supplied projection version must equal the current server version inside the same
 * transaction, otherwise validation fails closed with a stale conflict.</p>
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class ClinicalNoteValidationService {

    private final ClinicalNoteProjectionService projectionService;
    private final ClinicalNoteValidationRepository validationRepository;
    private final ClinicalNoteValidationFactRepository validationFactRepository;
    private final VisitRepository visitRepository;

    public ClinicalNoteValidationService(
            ClinicalNoteProjectionService projectionService,
            ClinicalNoteValidationRepository validationRepository,
            ClinicalNoteValidationFactRepository validationFactRepository,
            VisitRepository visitRepository) {
        this.projectionService = projectionService;
        this.validationRepository = validationRepository;
        this.validationFactRepository = validationFactRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public NoteValidationView validate(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            ValidateProjectionRequest request) {
        requireIdentity(visitId, userId, organizationId, request);
        lockAuthorizedVisit(visitId, organizationId);

        var existingByRequest = validationRepository.findByVisitIdAndValidationRequestId(
                visitId,
                request.validationId());
        if (existingByRequest.isPresent()) {
            return validateIdempotentRetry(existingByRequest.get(), userId, request);
        }

        NoteProjectionView current = projectionService.project(visitId, organizationId);
        String expectedVersion = request.projectionVersion().trim();
        if (!current.projectionVersion().equals(expectedVersion)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_CLINICAL_NOTE_PROJECTION_STALE");
        }

        var existingForProjection = validationRepository
                .findByVisitIdAndProjectionVersionAndValidatedByUserId(
                        visitId,
                        current.projectionVersion(),
                        userId);
        if (existingForProjection.isPresent()) {
            return view(existingForProjection.get());
        }

        ClinicalNoteValidationEntity validation = new ClinicalNoteValidationEntity(
                organizationId,
                visitId,
                request.validationId(),
                current.projectionVersion(),
                ClinicalNoteProjectionService.PROJECTION_SCHEMA_VERSION,
                current.maxFactSequence(),
                userId);

        try {
            validation = validationRepository.saveAndFlush(validation);
            List<ClinicalNoteValidationFactEntity> factRefs = snapshotFactRefs(
                    validation.getId(),
                    current);
            validationFactRepository.saveAllAndFlush(factRefs);
            return view(validation, factRefs);
        } catch (DataIntegrityViolationException exception) {
            return resolveConcurrentRetry(
                    visitId,
                    userId,
                    request,
                    current.projectionVersion(),
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public NoteValidationHistoryView history(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        List<NoteValidationView> validations = validationRepository
                .findByVisitIdOrderByValidatedAtDesc(visitId)
                .stream()
                .map(this::view)
                .toList();
        return new NoteValidationHistoryView(visitId, validations);
    }

    private NoteValidationView validateIdempotentRetry(
            ClinicalNoteValidationEntity existing,
            UUID userId,
            ValidateProjectionRequest request) {
        if (!existing.getValidatedByUserId().equals(userId)
                || !existing.getProjectionVersion().equals(request.projectionVersion().trim())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_CLINICAL_NOTE_VALIDATION_ID_REUSED");
        }
        return view(existing);
    }

    private NoteValidationView resolveConcurrentRetry(
            UUID visitId,
            UUID userId,
            ValidateProjectionRequest request,
            String projectionVersion,
            DataIntegrityViolationException exception) {
        var byRequest = validationRepository.findByVisitIdAndValidationRequestId(
                visitId,
                request.validationId());
        if (byRequest.isPresent()) {
            return validateIdempotentRetry(byRequest.get(), userId, request);
        }
        var byProjection = validationRepository
                .findByVisitIdAndProjectionVersionAndValidatedByUserId(
                        visitId,
                        projectionVersion,
                        userId);
        if (byProjection.isPresent()) {
            return view(byProjection.get());
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "AI_CLINICAL_NOTE_VALIDATION_CONFLICT",
                exception);
    }

    private List<ClinicalNoteValidationFactEntity> snapshotFactRefs(
            UUID validationId,
            NoteProjectionView projection) {
        List<ClinicalNoteValidationFactEntity> references = new ArrayList<>();
        projection.sections().forEach(section -> {
            for (int index = 0; index < section.entries().size(); index++) {
                var entry = section.entries().get(index);
                references.add(new ClinicalNoteValidationFactEntity(
                        validationId,
                        entry.factId(),
                        entry.factSequence(),
                        section.code().name(),
                        index));
            }
        });
        return references;
    }

    private NoteValidationView view(ClinicalNoteValidationEntity validation) {
        return view(
                validation,
                validationFactRepository.findByValidationIdOrderBySectionCodeAscPositionNoAsc(
                        validation.getId()));
    }

    private NoteValidationView view(
            ClinicalNoteValidationEntity validation,
            List<ClinicalNoteValidationFactEntity> factRefs) {
        List<ValidatedFactRefView> facts = factRefs.stream()
                .map(ref -> new ValidatedFactRefView(
                        ref.getFactId(),
                        ref.getFactSequence(),
                        ref.getSectionCode(),
                        ref.getPositionNo()))
                .toList();
        return new NoteValidationView(
                validation.getId(),
                validation.getValidationRequestId(),
                validation.getVisitId(),
                validation.getProjectionVersion(),
                validation.getProjectionSchemaVersion(),
                validation.getMaxFactSequence(),
                validation.getValidatedByUserId(),
                validation.getValidatedAt(),
                facts);
    }

    private void lockAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        if (visitId == null || organizationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI_CLINICAL_NOTE_VALIDATION_IDENTITY_INVALID");
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private void requireIdentity(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            ValidateProjectionRequest request) {
        if (visitId == null || userId == null || organizationId == null || request == null
                || request.validationId() == null
                || request.projectionVersion() == null
                || request.projectionVersion().isBlank()
                || request.projectionVersion().length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI_CLINICAL_NOTE_VALIDATION_INVALID");
        }
    }
}
