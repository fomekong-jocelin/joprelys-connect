package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.ai.application.AiConsultationContract.ClarificationView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiRepeatedClarificationGuardTest {

    private final AiRepeatedClarificationGuard guard = new AiRepeatedClarificationGuard();
    private final AiClinicalMemoryManager memory = new AiClinicalMemoryManager();

    @Test
    void shouldSuppressAnExactlyResolvedQuestion() {
        AiConsultationSessionState state = state();
        ClarificationView resolved = new ClarificationView(
                UUID.randomUUID(),
                "symptoms",
                "Depuis combien de temps ?",
                "RESOLVED",
                List.of(),
                Instant.now(),
                "Trois jours",
                Instant.now());
        memory.recordResolvedClarification(state, resolved, "Trois jours");
        ParsedResponse parsed = response("symptoms", "Depuis combien de temps ?");

        ParsedResponse guarded = guard.enforce(parsed, state, "fr");

        assertFalse(guarded.needsClarification());
        assertNull(guarded.clarification());
        assertTrue(guarded.assistantMessage().contains("déjà enregistrée"));
    }

    @Test
    void shouldKeepAQuestionThatWasNotResolved() {
        AiConsultationSessionState state = state();
        ParsedResponse parsed = response("symptoms", "Quelle est l'intensité ?");

        ParsedResponse guarded = guard.enforce(parsed, state, "fr");

        assertTrue(guarded.needsClarification());
        assertTrue(guarded == parsed);
    }

    private ParsedResponse response(String field, String question) {
        return new ParsedResponse(
                List.of(),
                question,
                true,
                new ParsedClarification(field, question, List.of()));
    }

    private AiConsultationSessionState state() {
        return new AiConsultationSessionState(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(300), "fr");
    }
}
