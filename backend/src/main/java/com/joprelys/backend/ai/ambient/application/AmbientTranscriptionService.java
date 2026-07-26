package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientTranscriptContract.TranscriptItemView;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AmbientTranscriptionService {

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
        validateChunkId(chunkId);
        String hash = sha256(audio);
        var claim = chunkJournal.claim(
                visitId,
                userId,
                organizationId,
                chunkId,
                hash,
                chunkStartOffsetMs,
                contentType);
        if (claim.alreadyCompleted()) {
            return chunkJournal.completedItems(visitId, chunkId);
        }

        UUID claimToken = claim.claimToken();
        try {
            var diarized = diarizationPort.transcribe(audio, contentType, locale);
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
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(audio));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
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
