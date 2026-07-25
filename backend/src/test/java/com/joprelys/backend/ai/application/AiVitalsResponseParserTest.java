package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AiVitalsResponseParserTest {

    private final AiVitalsResponseParser parser =
            new AiVitalsResponseParser(new ObjectMapper());

    @Test
    void shouldParseSafeVitalsProposal() {
        var parsed = parser.parse("""
                {
                  "assistantMessage": "Cinq constantes détectées. Vérifiez-les avant l'enregistrement.",
                  "needsConfirmation": false,
                  "confirmationReason": "",
                  "vitals": {
                    "temperature": 38.4,
                    "pulse": 104,
                    "systolic": 132,
                    "diastolic": 84,
                    "spo2": 96
                  }
                }
                """);

        assertFalse(parsed.needsConfirmation());
        assertEquals(38.4, parsed.vitals().get("temperature"));
        assertEquals(132.0, parsed.vitals().get("systolic"));
        assertEquals(5, parsed.vitals().size());
    }

    @Test
    void shouldAllowClarificationWithoutUnsafeProposal() {
        var parsed = parser.parse("""
                {
                  "assistantMessage": "Vous avez dit tension douze sur huit. Pouvez-vous confirmer la valeur en mmHg ?",
                  "needsConfirmation": true,
                  "confirmationReason": "Blood-pressure shorthand is ambiguous.",
                  "vitals": {}
                }
                """);

        assertTrue(parsed.needsConfirmation());
        assertTrue(parsed.vitals().isEmpty());
    }

    @Test
    void shouldRejectUnknownVitalField() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "assistantMessage": "Test",
                          "needsConfirmation": false,
                          "confirmationReason": "",
                          "vitals": {"temperature": 37.2, "inventedScore": 42}
                        }
                        """));

        assertEquals("AI_OUTPUT_INVALID", exception.getReason());
    }

    @Test
    void shouldRejectOutOfApplicationRangeValue() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse("""
                        {
                          "assistantMessage": "Test",
                          "needsConfirmation": false,
                          "confirmationReason": "",
                          "vitals": {"spo2": 140}
                        }
                        """));

        assertEquals("AI_OUTPUT_INVALID", exception.getReason());
    }
}
