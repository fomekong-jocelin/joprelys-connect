package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiClinicalMemoryGovernanceTest {

    @Test
    void shouldExcludeRejectedProposalFromMemory() {
        AiConsultationSessionState state = state();
        AiRevisionManager revisions = new AiRevisionManager();
        var revision = revisions.createRevision(state, List.of(new ParsedChange(
                "symptoms",
                "SET",
                "Fièvre depuis deux jours",
                "Explicitement dicté.",
                "LOW")));

        revisions.decideRevision(state, revision.id(), "REJECT");

        assertTrue(state.draft.isEmpty());
        assertTrue(state.clinicalMemory.acceptedFacts.isEmpty());
        assertFalse(state.providerMessages.stream().anyMatch(message ->
                message.content().contains("Fièvre depuis deux jours")));
    }

    @Test
    void shouldAddAcceptedProposalToMemory() {
        AiConsultationSessionState state = state();
        AiRevisionManager revisions = new AiRevisionManager();
        var revision = revisions.createRevision(state, List.of(new ParsedChange(
                "symptoms",
                "SET",
                "Fièvre depuis deux jours",
                "Explicitement dicté.",
                "LOW")));

        revisions.decideRevision(state, revision.id(), "ACCEPT");

        assertEquals("Fièvre depuis deux jours", state.draft.get("symptoms"));
        assertEquals("Fièvre depuis deux jours",
                state.clinicalMemory.acceptedFacts.get("symptoms"));
        assertTrue(state.providerMessages.getLast().content().contains("Fièvre depuis deux jours"));
    }

    @Test
    void shouldRememberResolvedClarification() {
        AiConsultationSessionState state = state();
        AiProperties properties = new AiProperties(
                true, "openai", "openai", 30, 20, 0.35, "fr", null, null, null);
        AiClarificationManager clarifications = new AiClarificationManager(properties);
        clarifications.append(state, new AiClinicalResponseParser.ParsedClarification(
                "symptoms",
                "Depuis combien de temps ?",
                List.of()));
        UUID clarificationId = state.clarifications.getFirst().id();

        clarifications.resolve(state, clarificationId, "Depuis trois jours");

        assertEquals(1, state.clinicalMemory.answeredClarifications.size());
        assertEquals("Depuis trois jours",
                state.clinicalMemory.answeredClarifications.getFirst().answer());
        assertTrue(state.providerMessages.getLast().content().contains("Depuis trois jours"));
    }

    private AiConsultationSessionState state() {
        return new AiConsultationSessionState(
                UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(300), "fr");
    }
}
