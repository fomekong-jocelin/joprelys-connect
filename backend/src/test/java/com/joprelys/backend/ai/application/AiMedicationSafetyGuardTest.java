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

class AiMedicationSafetyGuardTest {

    private final AiMedicationSafetyGuard guard = new AiMedicationSafetyGuard(new ObjectMapper());

    @Test
    void shouldHoldPrescriptionWhenExactDrugIsDocumentedAsAllergy() {
        ParsedResponse result = guard.enforce(
                responseWithDrug("Amoxicilline"),
                Map.of("patient", Map.of("allergies", "Amoxicilline; latex")),
                "fr");

        assertTrue(result.needsClarification());
        assertEquals("prescription", result.clarification().field());
        assertTrue(result.assistantMessage().contains("allergie"));
        assertFalse(result.changes().stream().anyMatch(change -> "prescription".equals(change.field())));
    }

    @Test
    void shouldHoldExactDuplicateActiveMedication() {
        ParsedResponse result = guard.enforce(
                responseWithDrug("Amlodipine"),
                Map.of("activeMedications", List.of(Map.of(
                        "drugName", "Amlodipine",
                        "dosage", "5 mg"))),
                "en");

        assertTrue(result.needsClarification());
        assertTrue(result.assistantMessage().contains("already listed"));
    }

    @Test
    void shouldNotInferDrugClassConflictWithoutAuthoritativeReferential() {
        ParsedResponse original = responseWithDrug("Amoxicilline");

        ParsedResponse result = guard.enforce(
                original,
                Map.of("patient", Map.of("allergies", "Pénicilline")),
                "fr");

        assertEquals(original, result);
    }

    private ParsedResponse responseWithDrug(String drugName) {
        return new ParsedResponse(
                List.of(new ParsedChange(
                        "prescription",
                        "SET",
                        "[{\"drugName\":\"" + drugName + "\",\"dosage\":\"500 mg\"}]",
                        "Médicament explicitement dicté.",
                        "LOW")),
                "Prescription proposée.",
                false,
                null);
    }
}
