package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiPrescriptionAttributeGroundingTest {

    private final AiClinicalFactualityGuard guard =
            new AiClinicalFactualityGuard(new ObjectMapper());

    @Test
    void routeMustNotBeAddedWhenClinicianDidNotDictateIt() {
        ParsedResponse parsed = response(
                "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\",\"route\":\"voie orale\"}]",
                List.of("Paracétamol 1000 mg"));

        ParsedResponse checked = guard.enforce(
                parsed,
                "Je prescris du Paracétamol 1000 mg.",
                Map.of(),
                "DICTATION",
                "fr");

        assertThat(checked.changes()).isEmpty();
    }

    @Test
    void routeMayBeStructuredWhenItIsExplicitlyDictated() {
        ParsedResponse parsed = response(
                "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\",\"route\":\"voie orale\"}]",
                List.of("Paracétamol 1000 mg", "voie orale"));

        ParsedResponse checked = guard.enforce(
                parsed,
                "Je prescris du Paracétamol 1000 mg par voie orale.",
                Map.of(),
                "DICTATION",
                "fr");

        assertThat(checked.changes()).hasSize(1);
    }

    private ParsedResponse response(String value, List<String> evidence) {
        return new ParsedResponse(
                List.of(new ParsedChange(
                        "prescription",
                        "SET",
                        value,
                        "Prescription explicitement dictée.",
                        "LOW",
                        evidence)),
                "Prescription structurée.",
                false,
                null);
    }
}
