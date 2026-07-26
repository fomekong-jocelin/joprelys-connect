package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedSegment;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptLedgerView;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSource;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptSpeaker;
import com.joprelys.backend.ai.ambient.domain.AmbientTranscriptStatus;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AmbientTranscriptLedgerService {

    private static final int MAX_TEXT_LENGTH = 12_000;
    private static final int MAX_SOURCE_EVENT_ID_LENGTH = 200;

    private final AmbientTranscriptItemRepository transcriptRepository;
    private final VisitRepository visitRepository;

    public AmbientTranscriptLedgerService(
            AmbientTranscriptItemRepository transcriptRepository,
            VisitRepository visitRepository) {
        this.transcriptRepository = transcriptRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional
    public List<TranscriptItemView> appendDiarizedSegments(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            long chunkStartOffsetMs,
            String locale,
            List<DiarizedSegment> segments) {
        requireIdentity(visitId, userId, organizationId);
        String normalizedChunkId = normalizeChunkId(chunkId);
        String normalizedLocale = normalizeLocale(locale);
        if (chunkStartOffsetMs < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_OFFSET_INVALID");
        }

        VisitEntity visit = lockAuthorizedVisit(visitId, organizationId);
        List<DiarizedSegment> orderedSegments = sanitizeAndSortSegments(segments);
        long nextSequence = transcriptRepository.findMaximumSequence(visit.getId());
        List<AmbientTranscriptItemEntity> result = new ArrayList<>();

        for (int index = 0; index < orderedSegments.size(); index++) {
            DiarizedSegment segment = orderedSegments.get(index);
            String sourceEventId = sourceEventId(normalizedChunkId, segment, index);
            var existing = transcriptRepository.findByVisitIdAndSourceEventId(visitId, sourceEventId);
            if (existing.isPresent()) {
                result.add(existing.get());
                continue;
            }

            long startOffsetMs = chunkStartOffsetMs + secondsToMillis(segment.startSeconds());
            long endOffsetMs = chunkStartOffsetMs + secondsToMillis(segment.endSeconds());
            if (endOffsetMs < startOffsetMs) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_SEGMENT_INVALID");
            }

            AmbientTranscriptItemEntity item = new AmbientTranscriptItemEntity(
                    organizationId,
                    visitId,
                    ++nextSequence,
                    sourceEventId,
                    AmbientTranscriptSource.AMBIENT_DIARIZED,
                    semanticSpeaker(segment.speakerLabel()),
                    normalizeSpeakerLabel(segment.speakerLabel()),
                    normalizeText(segment.text()),
                    normalizedLocale,
                    startOffsetMs,
                    endOffsetMs,
                    AmbientTranscriptStatus.FINAL,
                    userId,
                    null);
            result.add(transcriptRepository.save(item));
        }

        return result.stream()
                .sorted(Comparator.comparingLong(AmbientTranscriptItemEntity::getStartOffsetMs)
                        .thenComparingLong(AmbientTranscriptItemEntity::getSequenceNo))
                .map(this::view)
                .toList();
    }

    @Transactional
    public TranscriptItemView appendCorrection(
            UUID visitId,
            UUID itemId,
            UUID userId,
            UUID organizationId,
            String correctionId,
            String speakerType,
            String correctedText) {
        requireIdentity(visitId, userId, organizationId);
        lockAuthorizedVisit(visitId, organizationId);

        String sourceEventId = correctionEventId(correctionId);
        AmbientTranscriptSpeaker speaker = explicitSpeaker(speakerType);
        var retry = transcriptRepository.findByVisitIdAndSourceEventId(visitId, sourceEventId);
        if (retry.isPresent()) {
            validateCorrectionRetry(retry.get(), itemId, speaker, correctedText);
            return view(retry.get());
        }

        AmbientTranscriptItemEntity original = transcriptRepository.findByIdAndVisitId(itemId, visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "AI_AMBIENT_TRANSCRIPT_ITEM_NOT_FOUND"));
        if (original.getStatus() != AmbientTranscriptStatus.FINAL) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_AMBIENT_TRANSCRIPT_ITEM_NOT_FINAL");
        }
        if (transcriptRepository.findByVisitIdAndSupersedesItemId(visitId, original.getId()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_AMBIENT_TRANSCRIPT_ITEM_SUPERSEDED");
        }

        String text = correctedText == null || correctedText.isBlank()
                ? original.getTranscriptText()
                : normalizeText(correctedText);
        long sequence = transcriptRepository.findMaximumSequence(visitId) + 1;

        AmbientTranscriptItemEntity correction = new AmbientTranscriptItemEntity(
                organizationId,
                visitId,
                sequence,
                sourceEventId,
                AmbientTranscriptSource.MANUAL_CORRECTION,
                speaker,
                "human:" + speaker.name(),
                text,
                original.getLocale(),
                original.getStartOffsetMs(),
                original.getEndOffsetMs(),
                AmbientTranscriptStatus.FINAL,
                userId,
                original.getId());
        try {
            return view(transcriptRepository.saveAndFlush(correction));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI_AMBIENT_TRANSCRIPT_CORRECTION_CONFLICT",
                    exception);
        }
    }

    @Transactional(readOnly = true)
    public TranscriptLedgerView listFinal(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        List<AmbientTranscriptItemEntity> finalItems = transcriptRepository
                .findByVisitIdAndStatusOrderByStartOffsetMsAscSequenceNoAsc(
                        visitId, AmbientTranscriptStatus.FINAL);
        Set<UUID> supersededIds = new HashSet<>();
        for (AmbientTranscriptItemEntity item : finalItems) {
            if (item.getSupersedesItemId() != null) {
                supersededIds.add(item.getSupersedesItemId());
            }
        }
        List<TranscriptItemView> effectiveItems = finalItems.stream()
                .filter(item -> !supersededIds.contains(item.getId()))
                .map(this::view)
                .toList();
        return new TranscriptLedgerView(visitId, effectiveItems);
    }

    @Transactional(readOnly = true)
    public TranscriptLedgerView listAudit(UUID visitId, UUID organizationId) {
        requireAuthorizedVisit(visitId, organizationId);
        List<TranscriptItemView> items = transcriptRepository
                .findByVisitIdOrderByStartOffsetMsAscSequenceNoAsc(visitId)
                .stream()
                .map(this::view)
                .toList();
        return new TranscriptLedgerView(visitId, items);
    }

    private void validateCorrectionRetry(
            AmbientTranscriptItemEntity existing,
            UUID expectedParentId,
            AmbientTranscriptSpeaker expectedSpeaker,
            String correctedText) {
        if (existing.getSource() != AmbientTranscriptSource.MANUAL_CORRECTION
                || !Objects.equals(existing.getSupersedesItemId(), expectedParentId)
                || existing.getSpeakerType() != expectedSpeaker) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_AMBIENT_CORRECTION_ID_REUSED");
        }
        if (correctedText != null
                && !correctedText.isBlank()
                && !existing.getTranscriptText().equals(normalizeText(correctedText))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_AMBIENT_CORRECTION_ID_REUSED");
        }
    }

    private VisitEntity lockAuthorizedVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        return visit;
    }

    private void requireAuthorizedVisit(UUID visitId, UUID organizationId) {
        if (visitId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_IDENTITY_INVALID");
        }
        VisitEntity visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
    }

    private List<DiarizedSegment> sanitizeAndSortSegments(List<DiarizedSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        return segments.stream()
                .filter(segment -> segment != null && segment.text() != null && !segment.text().isBlank())
                .sorted(Comparator.comparingDouble(DiarizedSegment::startSeconds)
                        .thenComparingDouble(DiarizedSegment::endSeconds))
                .toList();
    }

    private void requireIdentity(UUID visitId, UUID userId, UUID organizationId) {
        if (visitId == null || userId == null || organizationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_IDENTITY_INVALID");
        }
    }

    private String normalizeChunkId(String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_CHUNK_ID_REQUIRED");
        }
        String normalized = chunkId.trim();
        if (normalized.length() > 120 || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_CHUNK_ID_INVALID");
        }
        return normalized;
    }

    private String correctionEventId(String correctionId) {
        if (correctionId == null || correctionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_CORRECTION_ID_REQUIRED");
        }
        String normalized = correctionId.trim();
        if (normalized.length() > 150 || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_CORRECTION_ID_INVALID");
        }
        return "correction:" + normalized;
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "fr";
        }
        String normalized = locale.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (!normalized.matches("[a-z]{2,3}(?:-[a-z]{2})?")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_LOCALE_INVALID");
        }
        return normalized;
    }

    private String sourceEventId(String chunkId, DiarizedSegment segment, int index) {
        String segmentId = segment.sourceSegmentId() == null || segment.sourceSegmentId().isBlank()
                ? Integer.toString(index)
                : segment.sourceSegmentId().trim();
        String result = chunkId + ":" + segmentId;
        if (result.length() > MAX_SOURCE_EVENT_ID_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_EVENT_ID_INVALID");
        }
        return result;
    }

    private String normalizeText(String text) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_TEXT_INVALID");
        }
        return normalized;
    }

    private String normalizeSpeakerLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String normalized = label.trim();
        return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
    }

    private AmbientTranscriptSpeaker semanticSpeaker(String rawLabel) {
        if (rawLabel == null) {
            return AmbientTranscriptSpeaker.UNSPECIFIED;
        }
        String normalized = rawLabel.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("doctor") || normalized.equals("clinician") || normalized.equals("provider")) {
            return AmbientTranscriptSpeaker.DOCTOR;
        }
        if (normalized.equals("patient")) {
            return AmbientTranscriptSpeaker.PATIENT;
        }
        return AmbientTranscriptSpeaker.UNSPECIFIED;
    }

    private AmbientTranscriptSpeaker explicitSpeaker(String speakerType) {
        if (speakerType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_SPEAKER_REQUIRED");
        }
        try {
            AmbientTranscriptSpeaker speaker = AmbientTranscriptSpeaker.valueOf(
                    speakerType.trim().toUpperCase(Locale.ROOT));
            if (speaker == AmbientTranscriptSpeaker.UNSPECIFIED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_SPEAKER_MUST_BE_EXPLICIT");
            }
            return speaker;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_SPEAKER_INVALID");
        }
    }

    private long secondsToMillis(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0 || seconds > 86_400) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AMBIENT_SEGMENT_INVALID");
        }
        return Math.round(seconds * 1000.0);
    }

    private TranscriptItemView view(AmbientTranscriptItemEntity item) {
        return new TranscriptItemView(
                item.getId(),
                item.getSequenceNo(),
                item.getSourceEventId(),
                item.getSource().name(),
                item.getSpeakerType().name(),
                item.getSpeakerLabel(),
                item.getTranscriptText(),
                item.getLocale(),
                item.getStartOffsetMs(),
                item.getEndOffsetMs(),
                item.getStatus().name(),
                item.getSupersedesItemId(),
                item.getCreatedAt());
    }
}
