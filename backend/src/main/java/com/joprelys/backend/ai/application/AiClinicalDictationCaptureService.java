package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dictation capture path deliberately bypassing the transient AI consultation session.
 * A dictation segment becomes durable evidence before any clinical structuring happens.
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiClinicalDictationCaptureService {

    private final AiProvider provider;
    private final RealtimeClinicalIntakeService intakeService;

    public AiClinicalDictationCaptureService(
            AiProvider provider,
            RealtimeClinicalIntakeService intakeService) {
        this.provider = provider;
        this.intakeService = intakeService;
    }

    public IntakeView capture(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            byte[] audio,
            String contentType,
            String locale) {
        AiConsultationInputValidator.validateAudio(audio, contentType);
        AiTranscription transcription;
        try {
            transcription = provider.transcribeAudio(
                    audio,
                    contentType,
                    normalizeLocale(locale));
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_TRANSCRIPTION_UNAVAILABLE",
                    exception);
        }
        if (transcription == null || transcription.text() == null || transcription.text().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_TRANSCRIPTION_EMPTY");
        }
        return intakeService.ingest(
                visitId,
                userId,
                organizationId,
                RealtimeIntakeSource.CONSULTATION,
                "dictation:" + UUID.randomUUID(),
                null,
                transcription.text(),
                transcription.confidence());
    }

    private String normalizeLocale(String locale) {
        return "en".equalsIgnoreCase(locale) ? "en" : "fr";
    }
}
