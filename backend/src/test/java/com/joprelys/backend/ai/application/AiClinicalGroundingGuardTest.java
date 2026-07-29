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
                      "uncertainty": "LOW",
                      "evidence": ["toux sèche depuis trois jours"]
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Paracétamol","dosage":"1 g"}],
                      "reason": "Traitement proposé.",
                      "uncertainty": "LOW",
                      "evidence": ["toux sèche depuis trois jours"]
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
                "DICTATION",
                "fr");

        assertEquals(1, grounded.changes().size());
        assertEquals("symptoms", grounded.changes().getFirst().field());
        assertTrue(grounded.assistantMessage().contains("Aucun médicament"));
    }

    @Test
    void shouldKeepMedicationExplicitlyDictatedByClinician() {
        var parsed = prescription("Paracétamol", "1 g", "Paracétamol un gramme trois fois par jour");

        var grounded = guard.enforce(
                parsed,
                "Je prescris du Paracétamol un gramme trois fois par jour.",
                null,
                "REALTIME",
                "fr");

        assertEquals(1, grounded.changes().size());
        assertEquals("prescription", grounded.changes().getFirst().field());
        assertEquals(parsed, grounded);
    }

    @Test
    void dictationMayStructureExplicitMedicationWithoutRepeatingPrescriptionVerb() {
        var parsed = parser.parse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": [{"drugName":"Paracétamol","dosage":"1000 mg","frequency":"matin midi soir","route":"voie orale"}],
                    "reason": "Médicament dicté par le médecin.",
                    "uncertainty": "LOW",
                    "evidence": ["Paracétamol 1000mg", "matin midi soir", "voie orale"]
                  }],
                  "assistantMessage": "Prescription structurée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "Paracétamol 1000mg à prendre matin midi soir par voie orale.",
                null,
                "DICTATION",
                "fr");

        assertEquals(List.of("prescription"), grounded.changes().stream().map(change -> change.field()).toList());
    }

    @Test
    void realtimeMedicationHistoryMustNotBecomePrescriptionWithoutPrescribingIntent() {
        var parsed = parser.parse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": [{"drugName":"Paracétamol","dosage":"1000 mg"}],
                    "reason": "Médicament entendu.",
                    "uncertainty": "LOW",
                    "evidence": ["Paracétamol 1000 mg"]
                  }],
                  "assistantMessage": "Prescription structurée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(
                parsed,
                "Le patient dit : je prends du Paracétamol 1000 mg depuis hier.",
                null,
                "REALTIME",
                "fr");

        assertTrue(grounded.changes().isEmpty());
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
                "REALTIME",
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
                      "uncertainty": "LOW",
                      "evidence": ["Cinq cents milligrammes trois fois par jour"]
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
                "CLARIFICATION",
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
                    "uncertainty": "LOW",
                    "evidence": ["dry cough"]
                  }],
                  "assistantMessage": "Medication added.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        var grounded = guard.enforce(parsed, "The patient has a dry cough.", null, "REALTIME", "en");

        assertTrue(grounded.changes().isEmpty());
        assertTrue(grounded.assistantMessage().contains("No medication"));
    }

    private AiClinicalResponseParser.ParsedResponse prescription(String drug, String dosage, String evidence) {
        return parser.parse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": [{"drugName":"%s","dosage":"%s","frequency":"3 fois par jour"}],
                    "reason": "Prescription explicitement dictée.",
                    "uncertainty": "LOW",
                    "evidence": ["%s"]
                  }],
                  "assistantMessage": "Prescription préparée pour validation.",
                  "needsClarification": false,
                  "clarification": null
                }
                """.formatted(drug, dosage, evidence));
    }
}
