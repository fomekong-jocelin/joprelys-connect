package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AiClinicalResponseParserStructuredTest {

    private final AiClinicalResponseParser parser =
            new AiClinicalResponseParser(new ObjectMapper());

    @Test
    void shouldAcceptStructuredPrescriptionAndVitals() {
        var parsed = parser.parse("""
                {
                  "changes": [
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": "[{\"drugName\":\"Paracétamol\",\"dosage\":\"1 g\",\"frequency\":\"3 fois par jour\",\"duration\":\"5 jours\"}]",
                      "reason": "Prescription dictée par le médecin.",
                      "uncertainty": "LOW"
                    },
                    {
                      "field": "vitals",
                      "operation": "SET",
                      "value": "{\"temperature\":38.2,\"systolic\":128,\"diastolic\":76,\"spo2\":97}",
                      "reason": "Constantes dictées.",
                      "uncertainty": "LOW"
                    }
                  ],
                  "assistantMessage": "J'ai préparé l'ordonnance et les constantes.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        assertEquals(2, parsed.changes().size());
        assertTrue(parsed.changes().get(0).proposedValue().contains("Paracétamol"));
        assertTrue(parsed.changes().get(1).proposedValue().contains("\"systolic\":128"));
    }

    @Test
    void shouldRejectUnknownPrescriptionAttribute() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "changes": [
                            {
                              "field": "prescription",
                              "operation": "SET",
                              "value": "[{\"drugName\":\"Amoxicilline\",\"autonomousDecision\":true}]",
                              "reason": "Test.",
                              "uncertainty": "LOW"
                            }
                          ],
                          "assistantMessage": "Test",
                          "needsClarification": false,
                          "clarification": null
                        }
                        """));

        assertEquals("AI_CHANGE_INVALID", exception.getReason());
    }

    @Test
    void shouldRejectUnknownVitalAttribute() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "changes": [
                            {
                              "field": "vitals",
                              "operation": "SET",
                              "value": "{\"temperature\":37.2,\"clinicalScoreInvented\":99}",
                              "reason": "Test.",
                              "uncertainty": "LOW"
                            }
                          ],
                          "assistantMessage": "Test",
                          "needsClarification": false,
                          "clarification": null
                        }
                        """));

        assertEquals("AI_CHANGE_INVALID", exception.getReason());
    }

    @Test
    void shouldAllowClarificationOnVitals() {
        var parsed = parser.parse("""
                {
                  "changes": [],
                  "assistantMessage": "Confirmez-vous une tension de 120 sur 80 millimètres de mercure ?",
                  "needsClarification": true,
                  "clarification": {
                    "field": "vitals",
                    "question": "Confirmez-vous une tension de 120 sur 80 mmHg ?",
                    "options": []
                  }
                }
                """);

        assertTrue(parsed.needsClarification());
        assertEquals("vitals", parsed.clarification().field());
    }
}
