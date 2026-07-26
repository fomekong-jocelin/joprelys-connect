package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import com.joprelys.backend.ai.domain.AiMessage;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiClinicalMemoryManagerTest {

    private final AiClinicalMemoryManager manager = new AiClinicalMemoryManager();

    @Test
    void shouldPublishOnlyAcceptedDraftValuesToProviderMemory() {
        AiConsultationSessionState state = state();
        state.draft.put("symptoms", "Toux sèche depuis trois jours");
        state.draft.put("diagnosis", "");
        state.providerMessages.add(AiMessage.user("historique"));

        manager.synchronizeAcceptedDraft(state);

        assertEquals(Map.of("symptoms", "Toux sèche depuis trois jours"),
                state.clinicalMemory.acceptedFacts);
        AiMessage memory = state.providerMessages.getLast();
        assertEquals(AiMessage.Role.SYSTEM, memory.role());
        assertTrue(memory.content().contains("symptoms: Toux sèche depuis trois jours"));
        assertFalse(memory.content().contains("diagnosis"));
    }

    @Test
    void shouldRememberResolvedQuestionAndReplacePreviousAnswer() {
        AiConsultationSessionState state = state();
        ClarificationView question = clarification("symptoms", "Depuis combien de temps ?");

        manager.recordResolvedClarification(state, question, "Deux jours");
        manager.recordResolvedClarification(state, question, "Trois jours");

        assertEquals(1, state.clinicalMemory.answeredClarifications.size());
        assertEquals("Trois jours",
                state.clinicalMemory.answeredClarifications.getFirst().answer());
        assertTrue(manager.wasAlreadyAnswered(state, "symptoms", "Depuis combien de temps ?"));
        assertTrue(state.providerMessages.getLast().content().contains("Trois jours"));
        assertFalse(state.providerMessages.getLast().content().contains("Deux jours"));
    }

    @Test
    void shouldKeepOnlyOneCurrentMemorySystemMessage() {
        AiConsultationSessionState state = state();
        state.draft.put("symptoms", "Fièvre");
        manager.synchronizeAcceptedDraft(state);
        state.draft.put("clinicalExam", "Température 38,4 °C");
        manager.synchronizeAcceptedDraft(state);

        List<AiMessage> memoryMessages = state.providerMessages.stream()
                .filter(message -> message.role() == AiMessage.Role.SYSTEM)
                .filter(message -> message.content().startsWith("JOPRELYS_GOVERNED_MEMORY"))
                .toList();

        assertEquals(1, memoryMessages.size());
        assertTrue(memoryMessages.getFirst().content().contains("clinicalExam"));
    }

    private AiConsultationSessionState state() {
        return new AiConsultationSessionState(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(300), "fr");
    }

    private ClarificationView clarification(String field, String question) {
        return new ClarificationView(
                UUID.randomUUID(),
                field,
                question,
                "RESOLVED",
                List.of(),
                Instant.now(),
                "",
                Instant.now());
    }
}
