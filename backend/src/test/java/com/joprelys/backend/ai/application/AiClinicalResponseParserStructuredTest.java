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
    void shouldAcceptStructuredChangesWithEvidence() {
        var parsed = parser.parse("""
                {
                  "changes": [
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Medication-A","dosage":"10 mg"}],
                      "reason": "Explicitly dictated.",
                      "uncertainty": "LOW",
                      "evidence": ["Medication-A 10 mg"]
                    },
                    {
                      "field": "vitals",
                      "operation": "SET",
                      "value": {"pulse":72},
                      "reason": "Explicitly dictated.",
                      "uncertainty": "LOW",
                      "evidence": ["pulse 72"]
                    }
                  ],
                  "assistantMessage": "Review the grounded proposals.",
                  "needsClarification": false,
                  "clarification": null
                }
                """);

        assertEquals(2, parsed.changes().size());
        assertTrue(parsed.changes().get(0).proposedValue().contains("Medication-A"));
        assertEquals(1, parsed.changes().get(0).evidence().size());
        assertTrue(parsed.changes().get(1).proposedValue().contains("\"pulse\":72"));
    }

    @Test
    void shouldRejectChangeWithoutEvidence() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "changes": [{
                            "field":"symptoms",
                            "operation":"SET",
                            "value":"Example symptom",
                            "reason":"Test",
                            "uncertainty":"LOW"
                          }],
                          "assistantMessage":"Test",
                          "needsClarification":false,
                          "clarification":null
                        }
                        """));
        assertEquals("AI_CHANGE_INVALID", exception.getReason());
    }

    @Test
    void shouldRejectLegacyDraftFallback() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "draft":{"symptoms":"Example"},
                          "assistantMessage":"Test",
                          "needsClarification":false,
                          "clarification":null
                        }
                        """));
        assertEquals("AI_OUTPUT_INVALID", exception.getReason());
    }

    @Test
    void shouldRejectUnknownPrescriptionAttribute() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "changes": [{
                            "field":"prescription",
                            "operation":"SET",
                            "value":[{"drugName":"Medication-A","unsupportedFlag":true}],
                            "reason":"Test",
                            "uncertainty":"LOW",
                            "evidence":["Medication-A"]
                          }],
                          "assistantMessage":"Test",
                          "needsClarification":false,
                          "clarification":null
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
                          "changes": [{
                            "field":"vitals",
                            "operation":"SET",
                            "value":{"pulse":72,"inventedMetric":99},
                            "reason":"Test",
                            "uncertainty":"LOW",
                            "evidence":["pulse 72"]
                          }],
                          "assistantMessage":"Test",
                          "needsClarification":false,
                          "clarification":null
                        }
                        """));
        assertEquals("AI_CHANGE_INVALID", exception.getReason());
    }

    @Test
    void shouldAllowClarificationOnVitals() {
        var parsed = parser.parse("""
                {
                  "changes": [],
                  "assistantMessage": "Please clarify the value.",
                  "needsClarification": true,
                  "clarification": {
                    "field": "vitals",
                    "question": "Please repeat the value.",
                    "options": []
                  }
                }
                """);
        assertTrue(parsed.needsClarification());
        assertEquals("vitals", parsed.clarification().field());
    }
}
