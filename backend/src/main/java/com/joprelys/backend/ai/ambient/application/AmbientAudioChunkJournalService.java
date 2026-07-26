package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.DiarizedSegment;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.domain.AmbientAudioChunkStatus;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkEntity;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkRepository;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientTranscriptItemRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AmbientAudioChunkJournalService {

    private static final Duration STALE_PROCESSING_AFTER = Duration.ofMinutes(2);

    private final AmbientAudioChunkRepository chunkRepository;
    private final AmbientTranscriptItemRepository transcriptRepository;
    private final VisitRepository visitRepository;
    private final AmbientTranscriptLedgerService ledgerService;

    public AmbientAudioChunkJournalService(
            AmbientAudioChunkRepository chunkRepository,
            AmbientTranscriptItemRepository transcriptRepository,
            VisitRepository visitRepository,
            AmbientTranscriptLedgerService ledgerService) {
        this.chunkRepository = chunkRepository;
        this.transcriptRepository = transcriptRepository;
        this.visitRepository = visitRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public ChunkClaim claim(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            String audioSha256,
            long startOffsetMs,
            String contentType) {
        lockVisit(visitId, organizationId);
        Instant now = Instant.now();
        var existing = chunkRepository.findByVisitIdAndChunkId(visitId, chunkId);
        if (existing.isPresent()) {
            AmbientAudioChunkEntity chunk = existing.get();
            validateSamePayload(chunk, audioSha256, startOffsetMs);
            if (chunk.getStatus() == AmbientAudioChunkStatus.COMPLETED) {
                return new ChunkClaim(true);
            }
            if (chunk.getStatus() == AmbientAudioChunkStatus.PROCESSING
                    && chunk.getClaimedAt().plus(STALE_PROCESSING_AFTER).isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_AMBIENT_CHUNK_PROCESSING");
            }
            chunk.reclaim(now);
            chunkRepository.save(chunk);
            return new ChunkClaim(false);
        }

        AmbientAudioChunkEntity created = new AmbientAudioChunkEntity(
                organizationId,
                visitId,
                chunkId,
                audioSha256,
                startOffsetMs,
                contentType,
                userId,
                now);
        chunkRepository.save(created);
        return new ChunkClaim(false);
    }

    @Transactional
    public List<TranscriptItemView> complete(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            long startOffsetMs,
            String locale,
            List<DiarizedSegment> segments) {
        lockVisit(visitId, organizationId);
        AmbientAudioChunkEntity chunk = chunkRepository.findByVisitIdAndChunkId(visitId, chunkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "AI_AMBIENT_CHUNK_NOT_CLAIMED"));
        if (chunk.getStatus() == AmbientAudioChunkStatus.COMPLETED) {
            return completedItems(visitId, chunkId);
        }
        List<TranscriptItemView> items = ledgerService.appendDiarizedSegments(
                visitId,
                userId,
                organizationId,
                chunkId,
                startOffsetMs,
                locale,
                segments);
        chunk.complete(Instant.now());
        chunkRepository.save(chunk);
        return items;
    }

    @Transactional
    public void fail(UUID visitId, UUID organizationId, String chunkId, String errorCode) {
        lockVisit(visitId, organizationId);
        chunkRepository.findByVisitIdAndChunkId(visitId, chunkId).ifPresent(chunk -> {
            if (chunk.getStatus() != AmbientAudioChunkStatus.COMPLETED) {
                chunk.fail(errorCode);
                chunkRepository.save(chunk);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<TranscriptItemView> completedItems(UUID visitId, String chunkId) {
        return transcriptRepository
                .findByVisitIdAndSourceEventIdStartingWithOrderByStartOffsetMsAscSequenceNoAsc(
                        visitId, chunkId + ":")
                .stream()
                .map(item -> new TranscriptItemView(
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
                        item.getCreatedAt()))
                .toList();
    }

    private VisitEntity lockVisit(UUID visitId, UUID organizationId) {
        VisitEntity visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND"));
        if (!organizationId.equals(visit.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        }
        return visit;
    }

    private void validateSamePayload(
            AmbientAudioChunkEntity chunk,
            String audioSha256,
            long startOffsetMs) {
        if (!chunk.getAudioSha256().equals(audioSha256)
                || chunk.getStartOffsetMs() != startOffsetMs) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AI_AMBIENT_CHUNK_HASH_MISMATCH");
        }
    }

    public record ChunkClaim(boolean alreadyCompleted) {
    }
}
