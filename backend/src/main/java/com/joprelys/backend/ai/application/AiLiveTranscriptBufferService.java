package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tampon serveur du transcript clinique en cours de capture.
 *
 * <p>Le mobile envoie le transcript complet après chaque tour final. Le numéro de
 * séquence rend l'écriture monotone et idempotente : une reconnexion peut renvoyer
 * le dernier état sans dupliquer ni réordonner le texte.</p>
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiLiveTranscriptBufferService {

    private final ConcurrentMap<Key, Entry> entries = new ConcurrentHashMap<>();
    private final int sessionTtlMinutes;

    public AiLiveTranscriptBufferService(AiProperties properties) {
        this(properties.sessionTtlMinutes());
    }

    AiLiveTranscriptBufferService(int sessionTtlMinutes) {
        this.sessionTtlMinutes = Math.max(5, sessionTtlMinutes);
    }

    public Snapshot upsert(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String transcript,
            long sequence,
            String eventId) {
        AiConsultationInputValidator.validateText(transcript);
        if (sequence < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_REALTIME_SEQUENCE_INVALID");
        }
        String normalizedTranscript = transcript.trim();
        String normalizedEventId = normalizeEventId(eventId);
        Key key = new Key(visitId, userId, organizationId);
        Entry updated = entries.compute(key, (ignored, current) -> merge(
                current,
                normalizedTranscript,
                sequence,
                normalizedEventId));
        return updated.snapshot();
    }

    public Optional<Snapshot> get(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        Key key = new Key(visitId, userId, organizationId);
        Entry current = entries.get(key);
        if (current == null) {
            return Optional.empty();
        }
        if (current.expiresAt().isBefore(Instant.now())) {
            entries.remove(key, current);
            return Optional.empty();
        }
        return Optional.of(current.snapshot());
    }

    public void clear(
            UUID visitId,
            UUID userId,
            UUID organizationId) {
        entries.remove(new Key(visitId, userId, organizationId));
    }

    private Entry merge(
            Entry current,
            String transcript,
            long sequence,
            String eventId) {
        Instant expiresAt = Instant.now().plus(sessionTtlMinutes, ChronoUnit.MINUTES);
        if (current == null || current.expiresAt().isBefore(Instant.now())) {
            return new Entry(transcript, sequence, eventId, expiresAt);
        }
        if (sequence < current.sequence()) {
            return current;
        }
        if (sequence == current.sequence()) {
            if (transcript.equals(current.transcript())
                    && eventId.equals(current.eventId())) {
                return current;
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_REALTIME_SEQUENCE_CONFLICT");
        }
        if (!transcript.startsWith(current.transcript())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "AI_REALTIME_TRANSCRIPT_STALE");
        }
        return new Entry(transcript, sequence, eventId, expiresAt);
    }

    private String normalizeEventId(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return "";
        }
        String normalized = eventId.trim();
        if (normalized.length() > 200
                || !normalized.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_REALTIME_EVENT_ID_INVALID");
        }
        return normalized;
    }

    private record Key(UUID visitId, UUID userId, UUID organizationId) {
    }

    private record Entry(
            String transcript,
            long sequence,
            String eventId,
            Instant expiresAt) {

        Snapshot snapshot() {
            return new Snapshot(transcript, sequence, eventId, expiresAt);
        }
    }

    public record Snapshot(
            String transcript,
            long sequence,
            String eventId,
            Instant expiresAt) {
    }
}
