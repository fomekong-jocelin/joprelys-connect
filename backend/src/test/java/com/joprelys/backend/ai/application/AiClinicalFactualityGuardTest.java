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

class AiClinicalFactualityGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiClinicalFactualityGuard guard =
            new AiClinicalFactualityGuard(objectMapper);

    @Test
    void shouldKeepExactGroundedSymptom() {
        ParsedResponse response = response(new ParsedChange(
                "symptoms",
                "SET",
                "toux depuis trois jours",
                "Explicit source",
                "LOW",
                List.of("toux depuis trois jours")));

        ParsedResponse checked = guard.enforce(
                response,
                "toux depuis trois jours",
                Map.of(),
                "AUDIO",
                "fr");

        assertEquals(1, checked.changes().size());
    }

    @Test
    void shouldBlockInventedDiagnosisEvenWithValidJson() {
        ParsedResponse response = response(new ParsedChange(
                "diagnosis",
                "SET",
                "condition-x",
                "Model inference",
                "LOW",
                List.of("toux depuis trois jours")));

        ParsedResponse checked = guard.enforce(
                response,
                "toux depuis trois jours",
                Map.of(),
                "REALTIME",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldBlockUnsupportedTermEvenInAdviceField() {
        ParsedResponse response = response(new ParsedChange(
                "advice",
                "SET",
                "hydratation et repos",
                "Model enrichment",
                "LOW",
                List.of("hydratation")));

        ParsedResponse checked = guard.enforce(
                response,
                "hydratation",
                Map.of(),
                "TEXT",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldBlockNegationReversal() {
        ParsedResponse response = response(new ParsedChange(
                "symptoms",
                "SET",
                "patient avec fievre",
                "Unsafe reformulation",
                "LOW",
                List.of("patient sans fievre")));

        ParsedResponse checked = guard.enforce(
                response,
                "patient sans fievre",
                Map.of(),
                "REALTIME",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldBlockAlteredNumericVital() {
        ParsedResponse response = response(new ParsedChange(
                "vitals",
                "SET",
                "{\"temperature\":38.2}",
                "Unsafe numeric change",
                "LOW",
                List.of("temperature 37,2")));

        ParsedResponse checked = guard.enforce(
                response,
                "temperature 37,2",
                Map.of(),
                "REALTIME",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldRejectFabricatedEvidenceQuote() {
        ParsedResponse response = response(new ParsedChange(
                "symptoms",
                "SET",
                "toux",
                "Fabricated evidence",
                "LOW",
                List.of("douleur thoracique")));

        ParsedResponse checked = guard.enforce(
                response,
                "toux",
                Map.of(),
                "TEXT",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldBlockClearWithoutExplicitRemovalIntent() {
        ParsedResponse response = response(new ParsedChange(
                "diagnosis",
                "CLEAR",
                null,
                "Unsafe clear",
                "LOW",
                List.of("diagnostic provisoire")));

        ParsedResponse checked = guard.enforce(
                response,
                "diagnostic provisoire",
                Map.of("diagnosis", "diagnostic provisoire"),
                "TEXT",
                "fr");

        assertTrue(checked.changes().isEmpty());
    }

    @Test
    void shouldAllowClearWithExplicitRemovalIntent() {
        ParsedResponse response = response(new ParsedChange(
                "diagnosis",
                "CLEAR",
                null,
                "Explicit clear",
                "LOW",
                List.of("supprime le diagnostic provisoire")));

        ParsedResponse checked = guard.enforce(
                response,
                "supprime le diagnostic provisoire",
                Map.of("diagnosis", "diagnostic provisoire"),
                "TEXT",
                "fr");

        assertEquals(1, checked.changes().size());
    }

    @Test
    void shouldAcceptHarmlessNumberUnitSpacingFromMedicalAsr() {
        ParsedResponse response = response(new ParsedChange(
                "prescription",
                "SET",
                "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\"}]",
                "Explicit prescription",
                "LOW",
                List.of("Paracétamol 1000mg")));

        ParsedResponse checked = guard.enforce(
                response,
                "Je lui prescris du Paracétamol 1000mg par voie orale.",
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        assertTrue(checked.changes().getFirst().proposedValue().contains("1000 mg"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepSafeMedicationFactsWhenOneFrequencyWasOverNormalized() throws Exception {
        ParsedResponse response = response(new ParsedChange(
                "prescription",
                "SET",
                "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\"},"
                        + "{\"drugName\":\"Vitafer\",\"frequency\":\"1 fois par jour\",\"route\":\"voie orale\"}]",
                "Explicit prescriptions",
                "MEDIUM",
                List.of("Paracétamol 1000mg", "Vitafer", "1 fois pas jour", "voie orale")));

        ParsedResponse checked = guard.enforce(
                response,
                "Je lui prescris du Paracétamol 1000mg. Vitafer 1 fois pas jour voie orale.",
                Map.of(),
                "CAPTURE",
                "fr");

        assertEquals(1, checked.changes().size());
        Object raw = objectMapper.readValue(checked.changes().getFirst().proposedValue(), Object.class);
        List<Map<String, Object>> lines = (List<Map<String, Object>>) raw;
        assertEquals(2, lines.size());
        assertEquals("1000 mg", lines.getFirst().get("dosage"));
        assertEquals("Vitafer", lines.get(1).get("drugName"));
        assertEquals("voie orale", lines.get(1).get("route"));
        assertFalse(lines.get(1).containsKey("frequency"));
    }

    @Test
    void shouldNeverReuseFreeFormAssistantMessage() {
        ParsedResponse response = new ParsedResponse(
                List.of(),
                "Le modele invente une conclusion clinique.",
                false,
                null);

        ParsedResponse checked = guard.enforce(
                response,
                "aucun nouvel element",
                Map.of(),
                "TEXT",
                "fr");

        assertFalse(checked.assistantMessage().contains("conclusion clinique"));
        assertTrue(checked.assistantMessage().contains("Aucun élément clinique"));
    }

    private ParsedResponse response(ParsedChange change) {
        return new ParsedResponse(
                List.of(change),
                "Model free-form summary",
                false,
                null);
    }
}
