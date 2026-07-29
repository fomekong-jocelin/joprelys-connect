package com.joprelys.backend.ai.realtime.application;

import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeEntity;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Removes transcript entries from the clinician working set without destroying
 * medico-legal traceability. Discarded rows remain auditable but are excluded from
 * every subsequent AI rebuild and from the recoverable transcript UI.
 */
@Service
public class RealtimeClinicalIntakeDiscardService {

    private static final String CONSUMED = "CONSUMED";
    private static final String DISCARDED = "DISCARDED";

    private final RealtimeClinicalIntakeRepository repository;
    private final VisitRepository visitRepository;

    public RealtimeClinicalIntakeDiscardService(
            RealtimeClinicalIntakeRepository repository,
            VisitRepository visitRepository) {
        this.repository = repository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public void discardOne(
            UUID visitId,
            UUID intakeId,
            UUID userId,
            UUID organizationId) {
        requireIdentity(visitId, intakeId, userId, organizationId);
        lockAuthorizedActiveVisit(visitId, organizationId);

        RealtimeClinicalIntakeEntity entity = repository
                .findByIdAndVisitIdAndSource(intakeId, visitId, RealtimeIntakeSource.CONSULTATION)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AI_REALTIME_INTAKE_NOT_FOUND"));

        if (CONSUMED.equals(entity.getCaptureStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_REALTIME_INTAKE_ALREADY_CONSUMED");
        }
        if (DISCARDED.equals(entity.getCaptureStatus())) {
            return;
        }

        entity.discard(userId, Instant.now());
        repository.save(entity);
    }

    @Transactional
    public int discardAll(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        if (visitId == null || userId == null || organizationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_IDENTITY_INVALID");
        }
        lockAuthorizedActiveVisit(visitId, organizationId);

        List<RealtimeClinicalIntakeEntity> active = repository
                .findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                        visitId,
                        RealtimeIntakeSource.CONSULTATION,
                        CONSUMED);
        Instant now = Instant.now();
        active.forEach(item -> item.discard(userId, now));
        repository.saveAll(active);
        return active.size();
    }

    private void requireIdentity(
            UUID visitId,
            UUID intakeId,
            UUID userId,
            UUID organizationId) {
        if (visitId == null || intakeId == null || userId == null || organizationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_IDENTITY_INVALID");
        }
    }

    private VisitEntity lockAuthorizedActiveVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        if (!"EN_COURS".equals(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "VISIT_NOT_ACTIVE");
        }
        return visit;
    }
}
