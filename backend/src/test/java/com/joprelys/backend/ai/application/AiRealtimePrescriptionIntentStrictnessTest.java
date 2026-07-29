package com.joprelys.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiRealtimePrescriptionIntentStrictnessTest {

    private final AiClinicalGroundingGuard guard = new AiClinicalGroundingGuard(new ObjectMapper());

    @Test
    void genericReferenceToPrescriptionMustNotAuthorizeRealtimeMedication() {
        ParsedResponse proposal = proposal();

        ParsedResponse checked = guard.enforce(
                proposal,
                "Le patient dit qu'on lui a parlé d'une prescription de Paracétamol 1000 mg.",
                null,
                "REALTIME",
                "fr");

        assertThat(checked.changes()).isEmpty();
    }

    @Test
    void infinitivePrescrireMustNotAuthorizeUnverifiedRealtimeSpeaker() {
        ParsedResponse proposal = proposal();

        ParsedResponse checked = guard.enforce(
                proposal,
                "On m'a conseillé de prescrire du Paracétamol 1000 mg.",
                null,
                "REALTIME",
                "fr");

        assertThat(checked.changes()).isEmpty();
    }

    @Test
    void explicitFirstPersonClinicianIntentMayAuthorizeGroundedRealtimeMedication() {
        ParsedResponse proposal = proposal();

        ParsedResponse checked = guard.enforce(
                proposal,
                "Je prescris du Paracétamol 1000 mg.",
                null,
                "REALTIME",
                "fr");

        assertThat(checked.changes()).hasSize(1);
    }

    private ParsedResponse proposal() {
        return new ParsedResponse(
                List.of(new ParsedChange(
                        "prescription",
                        "SET",
                        "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1000 mg\"}]",
                        "Médicament explicitement entendu.",
                        "LOW",
                        List.of("Paracétamol 1000 mg"))),
                "Prescription structurée.",
                false,
                null);
    }
}
