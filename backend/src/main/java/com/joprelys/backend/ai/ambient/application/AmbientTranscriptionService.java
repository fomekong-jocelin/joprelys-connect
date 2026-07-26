package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.KnownSpeakerReference;
import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import com.joprelys.backend.ai.ambient.infrastructure.persistence.AmbientAudioChunkEntity;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AmbientTranscriptionService {

    private static final int MAX_KNOWN_SPEAKERS = 4;
    private static final int MAX_REFERENCE_BYTES = 5 * 1024 * 1024;

    private final AmbientDiarizationPort diarizationPort;
    private final AmbientAudioChunkJournalService chunkJournal;

    public AmbientTranscriptionService(
            AmbientDiarizationPort diarizationPort,
            AmbientAudioChunkJournalService chunkJournal) {
        this.diarizationPort = diarizationPort;
        this.chunkJournal = chunkJournal;
    }

    public List<TranscriptItemView> ingestAudioChunk(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            long chunkStartOffsetMs,
            String locale,
            byte[] audio,
            String contentType) {
        return ingestAudioChunk(
                visitId,
                userId,
                organizationId,
                chunkId,
                chunkStartOffsetMs,
                locale,
                audio,
                contentType,
                List.of());
    }

    public List<TranscriptItemView> ingestAudioChunk(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            String chunkId,
            long chunkStartOffsetMs,
            String locale,
            byte[] audio,
            String contentType,
            List<KnownSpeakerReference> knownSpeakers) {
        validateChunkId(chunkId);
        List<KnownSpeakerReference> references = normalizeKnownSpeakers(knownSpeakers);
        String audioHash = sha256(audio);
        String contextHash = diarizationContextSha256(references);
        var claim = chunkJournal.claim(
                visitId,
                userId,
                organizationId,
                chunkId,
                audioHash,
                contextHash,
                chunkStartOffsetMs,
                contentType);
        if (claim.alreadyCompleted()) {
            return chunkJournal.completedItems(visitId, chunkId);
        }

        UUID claimToken = claim.claimToken();
        try {
            var diarized = references.isEmpty()
                    ? diarizationPort.transcribe(audio, contentType, locale)
                    : diarizationPort.transcribe(audio, contentType, locale, references);
            return chunkJournal.complete(
                    visitId,
                    userId,
                    organizationId,
                    chunkId,
                    claimToken,
                    chunkStartOffsetMs,
                    locale,
                    diarized.segments());
        } catch (RuntimeException exception) {
            chunkJournal.fail(
                    visitId,
                    organizationId,
                    chunkId,
                    claimToken,
                    errorCode(exception));
            throw exception;
        }
    }

    private List<KnownSpeakerReference> normalizeKnownSpeakers(List<KnownSpeakerReference> knownSpeakers) {
        if (knownSpeakers == null || knownSpeakers.isEmpty()) {
            return List.of();
        }
        if (knownSpeakers.size() > MAX_KNOWN_SPEAKERS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_KNOWN_SPEAKER_LIMIT_EXCEEDED");
        }
        Set<String> names = new HashSet<>();
        List<KnownSpeakerReference> result = new ArrayList<>();
        for (KnownSpeakerReference reference : knownSpeakers) {
            if (reference == null || reference.name() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_KNOWN_SPEAKER_INVALID");
            }
            String name = reference.name().trim().toLowerCase(Locale.ROOT);
            if (!name.matches("[a-z][a-z0-9_-]{0,31}") || !names.add(name)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_KNOWN_SPEAKER_INVALID");
            }
            byte[] referenceAudio = reference.audio();
            if (referenceAudio == null || referenceAudio.length == 0 || referenceAudio.length > MAX_REFERENCE_BYTES) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_KNOWN_SPEAKER_AUDIO_INVALID");
            }
            String referenceType = normalizeContentType(reference.contentType());
            if (referenceType.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_KNOWN_SPEAKER_AUDIO_INVALID");
            }
            result.add(new KnownSpeakerReference(name, referenceAudio.clone(), referenceType));
        }
        result.sort(Comparator.comparing(KnownSpeakerReference::name));
        return List.copyOf(result);
    }

    private String diarizationContextSha256(List<KnownSpeakerReference> references) {
        if (references.isEmpty()) {
            return AmbientAudioChunkEntity.EMPTY_DIARIZATION_CONTEXT_SHA256;
        }
        MessageDigest digest = sha256Digest();
        for (KnownSpeakerReference reference : references) {
            updateLengthPrefixed(digest, reference.name().getBytes(StandardCharsets.UTF_8));
            updateLengthPrefixed(digest, reference.contentType().getBytes(StandardCharsets.UTF_8));
            updateLengthPrefixed(digest, reference.audio());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void updateLengthPrefixed(MessageDigest digest, byte[] value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
        digest.update(value);
    }

    private void validateChunkId(String chunkId) {
        if (chunkId == null || chunkId.isBlank()
                || chunkId.length() > 120
                || !chunkId.matches("[A-Za-z0-9._:-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_CHUNK_ID_INVALID");
        }
    }

    private String sha256(byte[] audio) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI_AMBIENT_AUDIO_INVALID");
        }
        return HexFormat.of().formatHex(sha256Digest().digest(audio));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private String normalizeContentType(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    }

    private String errorCode(RuntimeException exception) {
        if (exception instanceof ResponseStatusException response
                && response.getReason() != null
                && !response.getReason().isBlank()) {
            return response.getReason();
        }
        return exception.getClass().getSimpleName();
    }
}
