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
    void promptsShouldRequestControlledRewritingAndExactEvidence() {
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "grammatically reformulate"));
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "must preserve every clinical fact"));
        assertTrue(AiClinicalFidelityContract.SYSTEM_INSTRUCTION.contains(
                "exact quotes copied from the CURRENT input"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "concise professional clinical sentences"));
        assertTrue(AiClinicalCapturePrompt.SYSTEM_PROMPT.contains(
                "may not add a new clinical token"));
    }
}
