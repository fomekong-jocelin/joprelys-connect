package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.TranscriptionView;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AiTranscriptionWorkflow {

    private static final Logger log = LoggerFactory.getLogger(AiTranscriptionWorkflow.class);

    private final AiProvider aiProvider;
    private final AiProperties properties;

    AiTranscriptionWorkflow(AiProvider aiProvider, AiProperties properties) {
        this.aiProvider = aiProvider;
        this.properties = properties;
    }

    TranscriptionView transcribe(
            AiConsultationSessionState state,
            byte[] audio,
            String contentType) {
        String normalizedMime = AiConsultationInputValidator.normalizeMimeType(contentType);
        try {
            AiTranscription transcription = aiProvider.transcribeAudio(
                    audio, normalizedMime, state.locale);
            if (transcription == null
                    || transcription.text() == null
                    || transcription.text().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
            }

            // A low ASR confidence must never destroy text that was actually heard.
            // Dictation is a clinician-reviewed workflow: the transcript is staged and
            // shown for correction before any clinical analysis can happen.
            logLowConfidence(transcription);
            return stage(state, transcription.text());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Échec transcription IA provider={}, octets={}",
                    properties.speechProvider(), audio.length);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI_UNAVAILABLE");
        }
    }

    TranscriptionView stage(AiConsultationSessionState state, String transcript) {
        state.pendingTranscript = limit(
                transcript.trim(),
                AiConsultationInputValidator.MAX_TRANSCRIPT_LENGTH);
        state.transcriptStatus = "PENDING_REVIEW";
        state.expiresAt = Instant.now().plus(
                properties.sessionTtlMinutes(), ChronoUnit.MINUTES);
        return new TranscriptionView(
                state.sessionId,
                state.pendingTranscript,
                state.transcriptStatus,
                state.expiresAt);
    }

    private void logLowConfidence(AiTranscription transcription) {
        Double confidence = transcription.confidence();
        double minimum = properties.minimumTranscriptionConfidence();
        if (confidence == null || minimum <= 0.0 || confidence >= minimum) {
            return;
        }
        log.warn(
                "Transcription IA de confiance faible conservée pour validation humaine provider={}, confiance={}, seuil={}",
                properties.speechProvider(),
                String.format("%.3f", confidence),
                String.format("%.3f", minimum));
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
