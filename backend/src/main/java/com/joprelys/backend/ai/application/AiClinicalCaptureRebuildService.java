package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService;
import com.joprelys.backend.ai.realtime.application.RealtimeClinicalIntakeService.IntakeView;
import com.joprelys.backend.ai.realtime.application.RealtimeIntakeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Deterministically rebuilds the recoverable AI working draft from the durable
 * clinician transcript. This is the recovery/finalization path and therefore does
 * not trust the transient browser queue nor a previous in-memory AI session.
 */
@Service
public class AiClinicalCaptureRebuildService {

    private static final int MAX_MODEL_CHUNK_CHARS = 8_000;

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
            consultationService.processRealtimeTranscript(
                    visitId,
                    userId,
                    organizationId,
                    chunk,
                    null);
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
