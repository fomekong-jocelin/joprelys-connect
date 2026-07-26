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
            validateConfidence(transcription);
            if (transcription == null
                    || transcription.text() == null
                    || transcription.text().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "AI_OUTPUT_INVALID");
            }
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

    private void validateConfidence(AiTranscription transcription) {
        Double confidence = transcription == null ? null : transcription.confidence();
        double minimum = properties.minimumTranscriptionConfidence();
        if (confidence == null || minimum <= 0.0 || confidence >= minimum) {
            return;
        }
        log.warn("Transcription IA rejetée pour confiance insuffisante provider={}, confiance={}",
                properties.speechProvider(), String.format("%.3f", confidence));
        throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY, "AI_TRANSCRIPTION_LOW_CONFIDENCE");
    }

    private String limit(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
