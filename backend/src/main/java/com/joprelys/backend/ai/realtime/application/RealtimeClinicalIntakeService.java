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

/**
 * Durable source of truth for clinician microphone transcripts.
 *
 * <p>Capture and clinical interpretation are deliberately separated. Every non-empty
 * transcript is persisted first, even when confidence is low or unavailable. A low
 * confidence score is metadata for human review; it never authorizes data loss and
 * never blocks subsequent capture.</p>
 */
@Service
public class RealtimeClinicalIntakeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeClinicalIntakeService.class);
    private static final double DEFAULT_CONFIDENCE_FLOOR = 0.35;
    private static final int MAX_TRANSCRIPT_LENGTH = 12_000;
    private static final String CONSUMED = "CONSUMED";

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
        return ingest(
                visitId,
                userId,
                organizationId,
                RealtimeIntakeSource.CONSULTATION,
                eventId,
                itemId,
                transcript,
                confidence);
    }

    @Transactional
    public IntakeView ingest(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            RealtimeIntakeSource source,
            String eventId,
            String itemId,
            String transcript,
            Double confidence) {
        requireIdentity(visitId, userId, organizationId, source);
        String normalizedEventId = requiredId(eventId, "AI_REALTIME_EVENT_ID_REQUIRED");
        String normalizedItemId = optionalId(itemId);
        String normalizedTranscript = normalizeTranscript(transcript);
        double normalizedConfidence = normalizeConfidence(confidence);

        lockAuthorizedVisit(visitId, organizationId);

        RealtimeClinicalIntakeEntity byEvent = repository
                .findByVisitIdAndSourceAndEventId(visitId, source, normalizedEventId)
                .orElse(null);
        if (byEvent != null) {
            requireSameEventPayload(byEvent, normalizedEventId, normalizedItemId, normalizedTranscript, normalizedConfidence);
            return view(byEvent);
        }

        if (normalizedItemId != null) {
            RealtimeClinicalIntakeEntity byItem = repository
                    .findByVisitIdAndSourceAndItemId(visitId, source, normalizedItemId)
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
                source,
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
                    .findByVisitIdAndSourceAndEventId(visitId, source, normalizedEventId)
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
                        .findByVisitIdAndSourceAndItemId(visitId, source, normalizedItemId)
                        .orElse(null);
                if (racedByItem != null) {
                    requireSameItemPayload(racedByItem, normalizedItemId, normalizedTranscript, normalizedConfidence);
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
        return listActive(visitId, organizationId, RealtimeIntakeSource.CONSULTATION);
    }

    @Transactional(readOnly = true)
    public List<IntakeView> list(
            UUID visitId,
            UUID organizationId,
            RealtimeIntakeSource source) {
        requireAuthorizedVisit(visitId, organizationId);
        requireSource(source);
        return repository.findByVisitIdAndSourceOrderBySequenceNoAsc(visitId, source)
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IntakeView> listActive(
            UUID visitId,
            UUID organizationId,
            RealtimeIntakeSource source) {
        requireAuthorizedVisit(visitId, organizationId);
        requireSource(source);
        return repository.findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                        visitId, source, CONSUMED)
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional
    public IntakeView correct(
            UUID visitId,
            UUID intakeId,
            UUID userId,
            UUID organizationId,
            String correctedTranscript) {
        requireIdentity(visitId, userId, organizationId, RealtimeIntakeSource.CONSULTATION);
        lockAuthorizedVisit(visitId, organizationId);
        RealtimeClinicalIntakeEntity entity = repository
                .findByIdAndVisitIdAndSource(intakeId, visitId, RealtimeIntakeSource.CONSULTATION)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AI_REALTIME_INTAKE_NOT_FOUND"));
        if (CONSUMED.equals(entity.getCaptureStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_REALTIME_INTAKE_ALREADY_CONSUMED");
        }
        String normalized = normalizeTranscript(correctedTranscript);
        if (!normalized.equals(entity.getTranscriptText())) {
            entity.correctTranscript(normalized, userId, Instant.now());
            repository.save(entity);
        }
        return view(entity);
    }

    @Transactional
    public void markAnalyzed(
            UUID visitId,
            UUID organizationId,
            RealtimeIntakeSource source) {
        requireAuthorizedVisit(visitId, organizationId);
        requireSource(source);
        Instant now = Instant.now();
        List<RealtimeClinicalIntakeEntity> active = repository
                .findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                        visitId, source, CONSUMED);
        active.forEach(item -> item.markAnalyzed(now));
        repository.saveAll(active);
    }

    @Transactional
    public void consume(
            UUID visitId,
            UUID organizationId,
            RealtimeIntakeSource source) {
        requireAuthorizedVisit(visitId, organizationId);
        requireSource(source);
        Instant now = Instant.now();
        List<RealtimeClinicalIntakeEntity> active = repository
                .findByVisitIdAndSourceAndCaptureStatusNotOrderBySequenceNoAsc(
                        visitId, source, CONSUMED);
        active.forEach(item -> item.markConsumed(now));
        repository.saveAll(active);
    }

    private void requireSameEventPayload(
            RealtimeClinicalIntakeEntity existing,
            String eventId,
            String itemId,
            String transcript,
            double confidence) {
        boolean same = existing.getEventId().equals(eventId)
                && Objects.equals(existing.getItemId(), itemId)
                && sameOriginalContent(existing, transcript, confidence);
        if (!same) throw reusedId();
    }

    private void requireSameItemPayload(
            RealtimeClinicalIntakeEntity existing,
            String itemId,
            String transcript,
            double confidence) {
        boolean same = Objects.equals(existing.getItemId(), itemId)
                && sameOriginalContent(existing, transcript, confidence);
        if (!same) throw reusedId();
    }

    /** Retries must be compared with the immutable ASR text, not a later human correction. */
    private boolean sameOriginalContent(
            RealtimeClinicalIntakeEntity existing,
            String transcript,
            double confidence) {
        String immutableOriginal = existing.getOriginalTranscriptText() == null
                ? existing.getTranscriptText()
                : existing.getOriginalTranscriptText();
        return immutableOriginal.equals(transcript)
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
        String normalized = transcript.trim().replaceAll("\\s+", " ");
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
            log.warn("Realtime transcript persisted without ASR confidence; capture continues and text remains reviewable");
            return 0.0d;
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_REALTIME_TRANSCRIPTION_UNVERIFIED");
        }
        if (confidence < minimum) {
            log.warn(
                    "Low-confidence realtime transcript preserved confidence={} minimum={}",
                    String.format("%.3f", confidence),
                    String.format("%.3f", minimum));
        }
        return confidence;
    }

    private void requireIdentity(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            RealtimeIntakeSource source) {
        if (visitId == null || userId == null || organizationId == null || source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_IDENTITY_INVALID");
        }
    }

    private void requireSource(RealtimeIntakeSource source) {
        if (source == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_REALTIME_INTAKE_SOURCE_INVALID");
        }
    }

    private IntakeView view(RealtimeClinicalIntakeEntity entity) {
        double minimum = properties.minimumTranscriptionConfidence() > 0.0
                ? properties.minimumTranscriptionConfidence()
                : DEFAULT_CONFIDENCE_FLOOR;
        return new IntakeView(
                entity.getId(),
                entity.getVisitId(),
                entity.getSource(),
                entity.getSequenceNo(),
                entity.getEventId(),
                entity.getItemId(),
                entity.getTranscriptText(),
                entity.getOriginalTranscriptText(),
                entity.getConfidence(),
                entity.getConfidence() <= 0.0 || entity.getConfidence() < minimum,
                entity.getCorrectionCount(),
                entity.getCorrectedAt(),
                entity.getCaptureStatus(),
                entity.getReceivedAt());
    }

    public record IntakeView(
            UUID id,
            UUID visitId,
            RealtimeIntakeSource source,
            long sequence,
            String eventId,
            String itemId,
            String transcript,
            String originalTranscript,
            double confidence,
            boolean reviewRequired,
            int correctionCount,
            Instant correctedAt,
            String captureStatus,
            Instant receivedAt) {
    }
}
