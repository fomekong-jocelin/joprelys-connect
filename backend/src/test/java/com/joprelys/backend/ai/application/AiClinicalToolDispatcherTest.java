package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedClarification;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    void repeatedFieldFactsMustReachRevisionLayerInsteadOfBeingDropped() {
        var response = new ParsedResponse(
                List.of(change("symptoms"), change("symptoms")),
                "Deux faits du même champ.",
                false,
                null);

        var plan = dispatcher.dispatch(response);

        assertEquals(2, plan.changes().size());
        assertEquals(2, plan.invocations().size());
        assertEquals("symptoms", plan.changes().get(0).field());
        assertEquals("symptoms", plan.changes().get(1).field());
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
