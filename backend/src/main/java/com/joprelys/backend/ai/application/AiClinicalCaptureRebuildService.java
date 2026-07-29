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

    private static final int MAX_MODEL_CHUNK_CHARS = 8_000;
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

        for (String chunk : chunks(capture)) {
            // Confidence is deliberately not used as a discard gate here. The durable
            // transcript is evidence and remains visible to the clinician for correction.
            MessageView result = consultationService.processRealtimeTranscript(
                    visitId,
                    userId,
                    organizationId,
                    chunk,
                    null);

            /*
             * A provider can legitimately decide that one element of a mixed paragraph
             * needs clarification (for example a medication) while another element is
             * perfectly usable (for example a symptom). Continuous capture must never let
             * that ambiguity erase the safe facts. Only when the whole chunk yielded no
             * structured change do we retry its individual factual sentences.
             */
            if (result.changedFields().isEmpty()) {
                List<String> sentences = factualSentences(chunk);
                if (sentences.size() > 1) {
                    for (String sentence : sentences) {
                        consultationService.processRealtimeTranscript(
                                visitId,
                                userId,
                                organizationId,
                                sentence,
                                null);
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

    private List<String> chunks(List<IntakeView> capture) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (IntakeView item : capture) {
            String text = item.transcript() == null ? "" : item.transcript().trim();
            if (text.isBlank()) continue;
            if (current.length() > 0 && current.length() + 1 + text.length() > MAX_MODEL_CHUNK_CHARS) {
                result.add(current.toString());
                current.setLength(0);
            }
            if (text.length() > MAX_MODEL_CHUNK_CHARS) {
                flush(result, current);
                splitLongText(result, text);
                continue;
            }
            if (current.length() > 0) current.append('\n');
            current.append(text);
        }
        flush(result, current);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_CAPTURE_EMPTY");
        }
        return List.copyOf(result);
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

    private void splitLongText(List<String> result, String text) {
        int offset = 0;
        while (offset < text.length()) {
            int end = Math.min(text.length(), offset + MAX_MODEL_CHUNK_CHARS);
            if (end < text.length()) {
                int boundary = text.lastIndexOf(' ', end);
                if (boundary > offset + MAX_MODEL_CHUNK_CHARS / 2) end = boundary;
            }
            result.add(text.substring(offset, end).trim());
            offset = end;
            while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) offset++;
        }
    }

    private void flush(List<String> result, StringBuilder current) {
        if (current.length() > 0) {
            result.add(current.toString());
            current.setLength(0);
        }
    }
}
