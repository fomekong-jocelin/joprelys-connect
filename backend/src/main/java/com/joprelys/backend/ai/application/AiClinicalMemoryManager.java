package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.domain.AiMessage;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AiClinicalMemoryManager {

    private static final int MAX_ANSWERED_CLARIFICATIONS = 24;
    private static final String MEMORY_PREFIX = "JOPRELYS_GOVERNED_MEMORY\n";

    void synchronizeAcceptedDraft(AiConsultationSessionState state) {
        state.clinicalMemory.acceptedFacts.clear();
        state.draft.forEach((field, value) -> {
            if (value != null && !value.isBlank()) {
                state.clinicalMemory.acceptedFacts.put(field, value.trim());
            }
        });
        refreshProviderMemory(state);
    }

    void recordResolvedClarification(
            AiConsultationSessionState state,
            ClarificationView clarification,
            String answer) {
        if (clarification == null || answer == null || answer.isBlank()) {
            return;
        }
        String normalizedQuestion = normalize(clarification.question());
        state.clinicalMemory.answeredClarifications.removeIf(existing ->
                existing.field().equals(clarification.field())
                        && normalize(existing.question()).equals(normalizedQuestion));
        state.clinicalMemory.answeredClarifications.add(
                new AiClinicalConversationMemory.AnsweredClarification(
                        clarification.field(),
                        clarification.question().trim(),
                        answer.trim(),
                        Instant.now()));
        while (state.clinicalMemory.answeredClarifications.size() > MAX_ANSWERED_CLARIFICATIONS) {
            state.clinicalMemory.answeredClarifications.removeFirst();
        }
        refreshProviderMemory(state);
    }

    boolean wasAlreadyAnswered(
            AiConsultationSessionState state,
            String field,
            String question) {
        String normalizedQuestion = normalize(question);
        return state.clinicalMemory.answeredClarifications.stream().anyMatch(answered ->
                answered.field().equals(field)
                        && normalize(answered.question()).equals(normalizedQuestion));
    }

    Map<String, Object> snapshot(AiConsultationSessionState state) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!state.clinicalMemory.acceptedFacts.isEmpty()) {
            result.put("acceptedFacts", Map.copyOf(state.clinicalMemory.acceptedFacts));
        }
        if (!state.clinicalMemory.answeredClarifications.isEmpty()) {
            List<Map<String, Object>> answered = state.clinicalMemory.answeredClarifications.stream()
                    .map(item -> Map.<String, Object>of(
                            "field", item.field(),
                            "question", item.question(),
                            "answer", item.answer(),
                            "resolvedAt", item.resolvedAt().toString()))
                    .toList();
            result.put("answeredClarifications", answered);
        }
        result.put("memoryPolicy", Map.of(
                "acceptedDataOnly", true,
                "rejectedProposalsExcluded", true,
                "doNotRepeatAnsweredQuestions", true));
        return Map.copyOf(result);
    }

    private void refreshProviderMemory(AiConsultationSessionState state) {
        state.providerMessages.removeIf(message ->
                message.role() == AiMessage.Role.SYSTEM
                        && message.content().startsWith(MEMORY_PREFIX));
        state.providerMessages.add(AiMessage.system(serialize(state)));
    }

    private String serialize(AiConsultationSessionState state) {
        StringBuilder content = new StringBuilder(MEMORY_PREFIX)
                .append("Only clinician-accepted data may be used as memory. ")
                .append("Do not repeat answered questions unless the current turn contradicts the stored answer.\n");
        if (!state.clinicalMemory.acceptedFacts.isEmpty()) {
            content.append("Accepted facts:\n");
            state.clinicalMemory.acceptedFacts.forEach((field, value) ->
                    content.append("- ").append(field).append(": ").append(value).append('\n'));
        }
        if (!state.clinicalMemory.answeredClarifications.isEmpty()) {
            content.append("Resolved questions:\n");
            state.clinicalMemory.answeredClarifications.forEach(item -> content
                    .append("- [").append(item.field()).append("] ")
                    .append(item.question()).append(" => ").append(item.answer()).append('\n'));
        }
        return content.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
