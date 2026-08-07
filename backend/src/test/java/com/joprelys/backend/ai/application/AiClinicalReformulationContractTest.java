package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiClinicalReformulationContractTest {

    private final AiClinicalFactualityGuard guard =
            new AiClinicalFactualityGuard(new ObjectMapper());

    @Test
    void shouldAllowProfessionalGrammaticalLinkingWhenFactsStayGrounded() {
        ParsedResponse response = new ParsedResponse(
                List.of(new ParsedChange(
                        "symptoms",
                        "SET",
                        "Le patient rapporte une toux depuis trois jours",
                        "Controlled clinical wording",
                        "LOW",
                        List.of("toux depuis trois jours"))),
                "ignored",
                false,
                null);

        ParsedResponse checked = guard.enforce(
                response,
                "toux depuis trois jours",
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        assertEquals(
                "Le patient rapporte une toux depuis trois jours",
                checked.changes().getFirst().proposedValue());
    }

    @Test
    void shouldAllowDialogueAnswerToBecomeADeclarativeClinicalStatement() {
        String source = "Vous êtes essoufflé ? Oui, un peu, quand je monte les escaliers. "
                + "J'ai besoin de reprendre mon souffle.";
        ParsedResponse response = new ParsedResponse(
                List.of(new ParsedChange(
                        "symptoms",
                        "SET",
                        "Essoufflé un peu quand je monte les escaliers. Besoin de reprendre mon souffle.",
                        "Dialogue converted to a declarative note",
                        "LOW",
                        List.of(
                                "un peu, quand je monte les escaliers",
                                "besoin de reprendre mon souffle"))),
                "ignored",
                false,
                null);

        ParsedResponse checked = guard.enforce(
                response,
                source,
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        assertEquals(
                "Essoufflé un peu quand je monte les escaliers. Besoin de reprendre mon souffle.",
                checked.changes().getFirst().proposedValue());
        assertFalse(checked.changes().getFirst().proposedValue().contains("?"));
    }

    @Test
    void shouldAllowNegativeAnswerWithoutCopyingTheQuestion() {
        String source = "Avez-vous une douleur dans la poitrine ? "
                + "Aucune douleur, seulement une sensation d'oppression de temps en temps.";
        ParsedResponse response = new ParsedResponse(
                List.of(new ParsedChange(
                        "symptoms",
                        "SET",
                        "Aucune douleur dans la poitrine, seulement une sensation d'oppression de temps en temps.",
                        "Explicit negative answer",
                        "LOW",
                        List.of("Aucune douleur, seulement une sensation d'oppression de temps en temps"))),
                "ignored",
                false,
                null);

        ParsedResponse checked = guard.enforce(
                response,
                source,
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        assertEquals(
                "Aucune douleur dans la poitrine, seulement une sensation d'oppression de temps en temps.",
                checked.changes().getFirst().proposedValue());
    }

    @Test
    void shouldKeepExactEvidenceWhenAReformulationIntroducesMedicalMeaning() {
        ParsedResponse response = new ParsedResponse(
                List.of(new ParsedChange(
                        "symptoms",
                        "SET",
                        "Le patient présente une bronchite depuis trois jours",
                        "Unsafe medical enrichment",
                        "LOW",
                        List.of("toux depuis trois jours"))),
                "ignored",
                false,
                null);

        ParsedResponse checked = guard.enforce(
                response,
                "toux depuis trois jours",
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        assertEquals(
                "toux depuis trois jours",
                checked.changes().getFirst().proposedValue());
        assertFalse(checked.changes().getFirst().proposedValue().contains("bronchite"));
    }

    @Test
    void promptsShouldRequestDialogueToNoteRewritingAndExactEvidence() {
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "grammatically reformulate"));
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "must preserve every clinical fact"));
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "EXACT quotes copied from the CURRENT input"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "transform conversational speech into concise declarative clinical prose"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "Do NOT merely copy the dialogue line by line"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "Never convert an unanswered part of a multiple question"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "Structured fields are factual data, not prose"));
    }
}
