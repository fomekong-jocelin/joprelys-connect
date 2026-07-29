package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
final class AiClarificationManager {

    private final AiProperties properties;
    private final AiClinicalMemoryManager memoryManager = new AiClinicalMemoryManager();

    AiClarificationManager(AiProperties properties) {
        this.properties = properties;
    }

    void ensureNoPending(AiConsultationSessionState state) {
        if (hasPending(state)) {
            throw conflict("AI_CLARIFICATION_ANSWER_REQUIRED");
        }
    }

    boolean hasPending(AiConsultationSessionState state) {
        return state.clarifications.stream()
                .anyMatch(clarification -> "PENDING".equals(clarification.status()));
    }

    ClarificationView findPending(
            AiConsultationSessionState state,
            UUID clarificationId) {
        return state.clarifications.stream()
                .filter(clarification -> clarification.id().equals(clarificationId))
                .filter(clarification -> "PENDING".equals(clarification.status()))
                .findFirst()
                .orElseThrow(() -> conflict("AI_CLARIFICATION_NOT_PENDING"));
    }

    void resolve(
            AiConsultationSessionState state,
            UUID clarificationId,
            String answer) {
        for (int index = 0; index < state.clarifications.size(); index++) {
            ClarificationView clarification = state.clarifications.get(index);
            if (clarification.id().equals(clarificationId)
                    && "PENDING".equals(clarification.status())) {
                ClarificationView resolved = new ClarificationView(
                        clarification.id(),
                        clarification.field(),
                        clarification.question(),
                        "RESOLVED",
                        clarification.options(),
                        clarification.createdAt(),
                        answer,
                        Instant.now());
                state.clarifications.set(index, resolved);
                memoryManager.recordResolvedClarification(state, resolved, answer);
                return;
            }
        }
        throw conflict("AI_CLARIFICATION_NOT_PENDING");
    }

    void append(
            AiConsultationSessionState state,
            ParsedClarification clarification) {
        boolean duplicatePending = state.clarifications.stream()
                .filter(item -> "PENDING".equals(item.status()))
                .anyMatch(item -> item.field().equals(clarification.field())
                        && normalize(item.question()).equals(normalize(clarification.question())));
        if (duplicatePending) {
            return;
        }

        state.clarifications.add(new ClarificationView(
                UUID.randomUUID(),
                clarification.field(),
                clarification.question(),
                "PENDING",
                clarification.options(),
                Instant.now(),
                null,
                null));
        int maximum = Math.max(5, properties.maxConversationTurns());
        while (state.clarifications.size() > maximum) {
            int resolvedIndex = firstResolvedIndex(state.clarifications);
            state.clarifications.remove(resolvedIndex >= 0 ? resolvedIndex : 0);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ");
    }

    private int firstResolvedIndex(List<ClarificationView> clarifications) {
        for (int index = 0; index < clarifications.size(); index++) {
            if ("RESOLVED".equals(clarifications.get(index).status())) {
                return index;
            }
        }
        return -1;
    }

    private ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
