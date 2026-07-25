package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiClinicalGroundingGuardTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiClinicalResponseParser parser = new AiClinicalResponseParser(objectMapper);
    private final AiClinicalGroundingGuard guard = new AiClinicalGroundingGuard(objectMapper);

    @Test
    void shouldBlockMedicationInventedAfterOrdinaryClinicalDictation() {
        var parsed = parser.parse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Toux sèche depuis trois jours",
                      "reason": "Symptôme dicté.",
                      "uncertainty": "LOW"
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Paracétamol","dosage":"1 g"}],
                      "reason": "Traitement proposé.",
                      "uncertainty": "LOW"
                    }
                  ],
                  "assistantMessage": "J'ai ajouté le symptôme et le traitement.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "Le patient a une toux sèche depuis trois jours.",
                null,
                "fr");

        assertEquals(1, grounded.changes().size());
        assertEquals("symptoms", grounded.changes().getFirst().field());
        assertTrue(grounded.assistantMessage().contains("Aucun médicament"));
    }

    @Test
    void shouldKeepMedicationExplicitlyDictatedByClinician() {
        var parsed = parser.parse("""
                {
                  "changes": [
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Paracétamol","dosage":"1 g","frequency":"3 fois par jour"}],
                      "reason": "Prescription explicitement dictée.",
                      "uncertainty": "LOW"
                    }
                  ],
                  "assistantMessage": "Prescription préparée pour validation.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "Je prescris du Paracétamol un gramme trois fois par jour.",
                null,
                "fr");

        assertEquals(1, grounded.changes().size());
        assertEquals("prescription", grounded.changes().getFirst().field());
        assertEquals(parsed, grounded);
    }

    @Test
    void shouldBlockMedicationClarificationWhenNoMedicationWasMentioned() {
        var parsed = parser.parse("""
                {
                  "changes": [],
                  "assistantMessage": "Quelle dose de paracétamol souhaitez-vous ?",
                  "needsClarification": true,
                  "clarification": {
                    "field": "prescription",
                    "question": "Quelle dose de paracétamol souhaitez-vous ?",
                    "options": []
                  }
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "La douleur est à droite depuis ce matin.",
                null,
                "fr");

        assertFalse(grounded.needsClarification());
        assertNull(grounded.clarification());
        assertTrue(grounded.changes().isEmpty());
    }

    @Test
    void shouldAllowAnswerToExistingPrescriptionClarification() {
        var parsed = parser.parse("""
                {
                  "changes": [
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Amoxicilline","dosage":"500 mg","frequency":"3 fois par jour"}],
                      "reason": "Le médecin précise la dose demandée.",
                      "uncertainty": "LOW"
                    }
                  ],
                  "assistantMessage": "Dose précisée, à valider.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "Cinq cents milligrammes trois fois par jour.",
                "prescription",
                "fr");

        assertEquals(List.of("prescription"), grounded.changes().stream().map(change -> change.field()).toList());
    }

    @Test
    void shouldUseEnglishSafetyMessageForEnglishSession() {
        var parsed = parser.parse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": [{"drugName":"Paracetamol","dosage":"1 g"}],
                    "reason": "Suggested treatment.",
                    "uncertainty": "LOW"
                  }],
                  "assistantMessage": "Medication added.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(parsed, "The patient has a dry cough.", null, "en");

        assertTrue(grounded.changes().isEmpty());
        assertTrue(grounded.assistantMessage().contains("No medication"));
    }
}
