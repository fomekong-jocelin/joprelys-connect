package com.joprelys.backend.ai.realtime.application;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeEntity;
import com.joprelys.backend.ai.realtime.infrastructure.persistence.RealtimeClinicalIntakeRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RealtimeClinicalIntakeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeClinicalIntakeService.class);
    private static final double DEFAULT_CONFIDENCE_FLOOR = 0.35;
    private static final int MAX_TRANSCRIPT_LENGTH = 12_000;

    private final RealtimeClinicalIntakeRepository repository;
    private final VisitRepository visitRepository;
    private final AiProperties properties;

    public RealtimeClinicalIntakeService(
            RealtimeClinicalIntakeRepository repository,
            VisitRepository visitRepository,
            AiProperties properties) {
        this.repository = repository;
        this.visitRepository = visitRepository;
        this.properties = properties;
    }

    @Transactional
    public IntakeView ingest(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String eventId,
            String itemId,
            String transcript,
            Double confidence) {
        requireIdentity(visitId, userId, organizationId);
        String normalizedEventId = requiredId(eventId, "AI_REALTIME_EVENT_ID_REQUIRED");
        String normalizedItemId = optionalId(itemId);
        String normalizedTranscript = normalizeTranscript(transcript);
        double normalizedConfidence = normalizeConfidence(confidence);

        lockAuthorizedVisit(visitId, organizationId);

        RealtimeClinicalIntakeEntity byEvent = repository
                .findByVisitIdAndEventId(visitId, normalizedEventId)
                .orElse(null);
        if (byEvent != null) {
            requireSameEventPayload(
                    byEvent,
                    normalizedEventId,
                    normalizedItemId,
                    normalizedTranscript,
                    normalizedConfidence);
            return view(byEvent);
        }

        if (normalizedItemId != null) {
            RealtimeClinicalIntakeEntity byItem = repository
                    .findByVisitIdAndItemId(visitId, normalizedItemId)
                    .orElse(null);
            if (byItem != null) {
                requireSameItemPayload(byItem, normalizedItemId, normalizedTranscript, normalizedConfidence);
                return view(byItem);
            }
        }

        long sequence = repository.findMaximumSequence(visitId) + 1;
        RealtimeClinicalIntakeEntity entity = new RealtimeClinicalIntakeEntity(
                organizationId,
                visitId,
                sequence,
                normalizedEventId,
                normalizedItemId,
                normalizedTranscript,
                normalizedConfidence,
                userId);
        try {
            return view(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            RealtimeClinicalIntakeEntity racedByEvent = repository
                    .findByVisitIdAndEventId(visitId, normalizedEventId)
                    .orElse(null);
            if (racedByEvent != null) {
                requireSameEventPayload(
                        racedByEvent,
                        normalizedEventId,
                        normalizedItemId,
                        normalizedTranscript,
                        normalizedConfidence);
                return view(racedByEvent);
            }
            if (normalizedItemId != null) {
                RealtimeClinicalIntakeEntity racedByItem = repository
                        .findByVisitIdAndItemId(visitId, normalizedItemId)
                        .orElse(null);
                if (racedByItem != null) {
                    requireSameItemPayload(
                            racedByItem,
                            normalizedItemId,
                            normalizedTranscript,
                            normalizedConfidence);
                    return view(racedByItem);
                }
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_REALTIME_INTAKE_CONCURRENT_CONFLICT",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public List<IntakeView> list(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        return repository.findByVisitIdOrderBySequenceNoAsc(visitId)
                .stream()
                .map(this::view)
                .toList();
    }

    private void requireSameEventPayload(
            RealtimeClinicalIntakeEntity existing,
            String eventId,
            String itemId,
            String transcript,
            double confidence) {
        boolean same = existing.getEventId().equals(eventId)
                && Objects.equals(existing.getItemId(), itemId)
                && sameContent(existing, transcript, confidence);
        if (!same) throw reusedId();
    }

    private void requireSameItemPayload(
            RealtimeClinicalIntakeEntity existing,
            String itemId,
            String transcript,
            double confidence) {
        boolean same = Objects.equals(existing.getItemId(), itemId)
                && sameContent(existing, transcript, confidence);
        if (!same) throw reusedId();
    }

    private boolean sameContent(
            RealtimeClinicalIntakeEntity existing,
            String transcript,
            double confidence) {
        return existing.getTranscriptText().equals(transcript)
                && Math.abs(existing.getConfidence() - confidence) < 0.000001d;
    }

    private ResponseStatusException reusedId() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "AI_REALTIME_INTAKE_ID_REUSED");
    }

    private VisitEntity lockAuthorizedVisit(UUID visitId, UUID organizationId) {
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

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        if (visitId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_IDENTITY_INVALID");
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private String normalizeTranscript(String transcript) {
        if (transcript == null || transcript.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_TRANSCRIPT_REQUIRED");
        }
        String normalized = transcript.trim();
        if (normalized.length() > MAX_TRANSCRIPT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "AI_REALTIME_TRANSCRIPT_TOO_LARGE");
        }
        return normalized;
    }

    private String requiredId(String value, String errorCode) {
        String normalized = optionalId(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
        }
        return normalized;
    }

    private String optionalId(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > 200 || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_ID_INVALID");
        }
        return normalized;
    }

    private double normalizeConfidence(Double confidence) {
        double configured = properties.minimumTranscriptionConfidence();
        double minimum = configured > 0.0 ? configured : DEFAULT_CONFIDENCE_FLOOR;
        if (confidence == null) {
            log.warn("Realtime transcript persisted without ASR confidence; clinician review remains required");
            return 0.0d;
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_REALTIME_TRANSCRIPTION_UNVERIFIED");
        }
        if (confidence < minimum) {
            log.warn(
                    "Low-confidence realtime transcript persisted confidence={} minimum={}",
                    String.format("%.3f", confidence),
                    String.format("%.3f", minimum));
        }
        return confidence;
    }

    private void requireIdentity(UUID visitId, UUID userId, UUID organizationId) {
        if (visitId == null || userId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_IDENTITY_INVALID");
        }
    }

    private IntakeView view(RealtimeClinicalIntakeEntity entity) {
        Instant receivedAt = entity.getReceivedAt();
        return new IntakeView(
                entity.getId(),
                entity.getVisitId(),
                entity.getSequenceNo(),
                entity.getEventId(),
                entity.getItemId(),
                entity.getTranscriptText(),
                entity.getConfidence(),
                receivedAt);
    }

    public record IntakeView(
            UUID id,
            UUID visitId,
            long sequence,
            String eventId,
            String itemId,
            String transcript,
            double confidence,
            Instant receivedAt) {
    }
}
