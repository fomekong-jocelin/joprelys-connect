package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiClinicalToolDispatcherTest {

    private final AiClinicalToolDispatcher dispatcher = new AiClinicalToolDispatcher();

    @Test
    void shouldRouteStructuredClinicalChangesToExplicitTools() {
        var response = new ParsedResponse(
                List.of(
                        change("symptoms"),
                        change("prescription"),
                        change("labOrders"),
                        change("vitals")),
                "Propositions prêtes.",
                false,
                null);

        var plan = dispatcher.dispatch(response);

        assertEquals(List.of(
                AiClinicalToolDispatcher.ClinicalToolType.PROPOSE_CLINICAL_NOTE,
                AiClinicalToolDispatcher.ClinicalToolType.PROPOSE_PRESCRIPTION,
                AiClinicalToolDispatcher.ClinicalToolType.PROPOSE_LAB_ORDERS,
                AiClinicalToolDispatcher.ClinicalToolType.PROPOSE_VITALS),
                plan.invocations().stream().map(AiClinicalToolDispatcher.ClinicalToolInvocation::type).toList());
        assertEquals(4, plan.changes().size());
    }

    @Test
    void shouldRepresentClarificationAsDedicatedTool() {
        var clarification = new ParsedClarification(
                "vitals",
                "Confirmez-vous 120 sur 80 mmHg ?",
                List.of("Oui", "Non"));
        var response = new ParsedResponse(
                List.of(change("symptoms")),
                "Je vérifie une constante.",
                true,
                clarification);

        var plan = dispatcher.dispatch(response);

        assertEquals(AiClinicalToolDispatcher.ClinicalToolType.ASK_CLARIFICATION,
                plan.invocations().getLast().type());
        assertEquals("vitals", plan.clarification().field());
    }

    @Test
    void shouldRejectTwoActionsForSameClinicalFieldInOneTurn() {
        var response = new ParsedResponse(
                List.of(change("symptoms"), change("symptoms")),
                "Deux changements concurrents.",
                false,
                null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> dispatcher.dispatch(response));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("AI_TOOL_DUPLICATE_FIELD", exception.getReason());
    }

    private ParsedChange change(String field) {
        return new ParsedChange(
                field,
                "SET",
                "prescription".equals(field) ? "[{\"drugName\":\"Paracétamol\"}]"
                        : "labOrders".equals(field) ? "[\"NFS\"]"
                        : "vitals".equals(field) ? "{\"temperature\":38.2}"
                        : "Valeur clinique",
                "Dicté explicitement.",
                "LOW");
    }
}
