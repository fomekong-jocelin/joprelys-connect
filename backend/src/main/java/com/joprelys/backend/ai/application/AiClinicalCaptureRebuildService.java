package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deterministically rebuilds the recoverable AI working draft from the durable
 * clinician transcript. This is the recovery/finalization path and therefore does
 * not trust the transient browser queue nor a previous in-memory AI session.
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class AiClinicalCaptureRebuildService {

    private static final Logger log = LoggerFactory.getLogger(AiClinicalCaptureRebuildService.class);

    // Keep enough headroom under AiConsultationInputValidator.MAX_TRANSCRIPT_LENGTH.
    // A normal consultation now requires far fewer serial model round-trips than the
    // historical 8k chunks, while still keeping each extraction request bounded.
    private static final int MAX_MODEL_CHUNK_CHARS = 30_000;

    private final RealtimeClinicalIntakeService intakeService;
    private final AiConsultationService consultationService;

    public AiClinicalCaptureRebuildService(
            RealtimeClinicalIntakeService intakeService,
            AiConsultationService consultationService) {
        this.intakeService = intakeService;
        this.consultationService = consultationService;
    }

    public SessionView rebuild(
            UUID visitId,
            UUID userId,
            UUID organizationId,
            Map<String, String> clinicianDraft,
            String locale) {
        long started = System.nanoTime();
        List<IntakeView> capture = intakeService.listActive(
                visitId,
                organizationId,
                RealtimeIntakeSource.CONSULTATION);
        if (capture.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CAPTURE_EMPTY");
        }

        consultationService.startSession(
                visitId,
                userId,
                organizationId,
                clinicianDraft == null ? Map.of() : clinicianDraft,
                locale);

        List<String> chunks = chunks(capture);
        int transcriptChars = 0;
        for (String chunk : chunks) {
            transcriptChars += chunk.length();
            consultationService.processCaptureTranscript(
                    visitId,
                    userId,
                    organizationId,
                    chunk);
        }

        intakeService.markAnalyzed(
                visitId,
                organizationId,
                RealtimeIntakeSource.CONSULTATION);
        SessionView result = consultationService.getSession(visitId, userId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "AI_SESSION_EXPIRED"));
        log.info(
                "AI_CAPTURE_REBUILD chunks={} transcriptChars={} durationMs={} fields={}",
                chunks.size(),
                transcriptChars,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started),
                result.draft().size());
        return result;
    }

    /**
     * Regroupe les segments dans des requêtes bornées. Les segments ordinaires ne
     * sont pas coupés ; un segment exceptionnellement supérieur à la limite est
     * découpé sans perte afin qu'aucun appel au modèle ne dépasse la taille maximale.
     */
    private List<String> chunks(List<IntakeView> capture) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (IntakeView item : capture) {
            String text = item.transcript() == null ? "" : item.transcript().trim();
            if (text.isBlank()) continue;
            appendBounded(result, current, text);
        }
        flush(result, current);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CAPTURE_EMPTY");
        }
        return List.copyOf(result);
    }

    private void appendBounded(List<String> result, StringBuilder current, String text) {
        int offset = 0;
        while (offset < text.length()) {
            int separatorLength = current.length() == 0 ? 0 : 1;
            int available = MAX_MODEL_CHUNK_CHARS - current.length() - separatorLength;
            if (available <= 0) {
                flush(result, current);
                continue;
            }

            int remaining = text.length() - offset;
            int length = Math.min(available, remaining);
            if (separatorLength > 0) {
                current.append('\n');
            }
            current.append(text, offset, offset + length);
            offset += length;

            if (current.length() >= MAX_MODEL_CHUNK_CHARS) {
                flush(result, current);
            }
        }
    }

    private void flush(List<String> result, StringBuilder current) {
        if (current.length() > 0) {
            result.add(current.toString());
            current.setLength(0);
        }
    }
}
