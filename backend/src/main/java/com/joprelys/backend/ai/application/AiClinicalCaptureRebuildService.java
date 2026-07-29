package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
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

    /**
     * Large enough to preserve a meaningful consultation span in one reasoning call,
     * while still bounding request size for very long encounters.
     */
    private static final int MAX_MODEL_CHUNK_CHARS = 24_000;
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?;:])\\s+|[\\r\\n]+");

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

        for (CaptureChunk chunk : chunks(capture)) {
            MessageView result = consultationService.processCaptureTranscript(
                    visitId,
                    userId,
                    organizationId,
                    chunk.text(),
                    null,
                    chunk.source());

            /*
             * A provider can legitimately decide that one element of a mixed paragraph
             * needs clarification while another is usable. If the whole chunk produced
             * nothing, retry its factual sentences independently rather than discarding
             * the corpus. Provenance remains unchanged during the retry.
             */
            if (result != null && result.changedFields().isEmpty()) {
                List<String> sentences = factualSentences(chunk.text());
                if (sentences.size() > 1) {
                    for (String sentence : sentences) {
                        consultationService.processCaptureTranscript(
                                visitId,
                                userId,
                                organizationId,
                                sentence,
                                null,
                                chunk.source());
                    }
                }
            }
        }

        intakeService.markAnalyzed(
                visitId,
                organizationId,
                RealtimeIntakeSource.CONSULTATION);
        return consultationService.getSession(visitId, userId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "AI_SESSION_EXPIRED"));
    }

    private List<CaptureChunk> chunks(List<IntakeView> capture) {
        List<CaptureChunk> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentSource = null;

        for (IntakeView item : capture) {
            String text = item.transcript() == null ? "" : item.transcript().trim();
            if (text.isBlank()) continue;
            String source = captureSource(item);

            if (current.length() > 0 && !source.equals(currentSource)) {
                flush(result, current, currentSource);
                currentSource = null;
            }
            if (currentSource == null) currentSource = source;

            if (current.length() > 0 && current.length() + 1 + text.length() > MAX_MODEL_CHUNK_CHARS) {
                flush(result, current, currentSource);
            }
            if (text.length() > MAX_MODEL_CHUNK_CHARS) {
                flush(result, current, currentSource);
                splitLongText(result, text, source);
                currentSource = null;
                continue;
            }
            if (current.length() > 0) current.append('\n');
            current.append(text);
        }
        flush(result, current, currentSource);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CAPTURE_EMPTY");
        }
        return List.copyOf(result);
    }

    private String captureSource(IntakeView item) {
        String eventId = item.eventId();
        return eventId != null && eventId.startsWith("dictation:") ? "DICTATION" : "REALTIME";
    }

    private List<String> factualSentences(String chunk) {
        if (chunk == null || chunk.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String sentence : SENTENCE_BOUNDARY.split(chunk.trim())) {
            String normalized = sentence.trim();
            if (!normalized.isBlank()) result.add(normalized);
        }
        return List.copyOf(result);
    }

    private void splitLongText(List<CaptureChunk> result, String text, String source) {
        int offset = 0;
        while (offset < text.length()) {
            int end = Math.min(text.length(), offset + MAX_MODEL_CHUNK_CHARS);
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > offset + MAX_MODEL_CHUNK_CHARS / 2) end = boundary;
            }
            result.add(new CaptureChunk(text.substring(offset, end).trim(), source));
            offset = end;
            while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) offset++;
        }
    }

    private void flush(List<CaptureChunk> result, StringBuilder current, String source) {
        if (current.length() > 0) {
            result.add(new CaptureChunk(current.toString(), source == null ? "REALTIME" : source));
            current.setLength(0);
        }
    }

    private record CaptureChunk(String text, String source) {
    }
}
