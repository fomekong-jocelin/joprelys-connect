package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedChange;
import com.joprelys.backend.ai.application.AiClinicalResponseParser.ParsedResponse;
import com.joprelys.backend.medication.reference.MedicationConcept;
import com.joprelys.backend.medication.reference.MedicationReferenceDuplicateDetector;
import com.joprelys.backend.medication.reference.MedicationReferencePort;
import com.joprelys.backend.medication.reference.MedicationReferenceProperties;
import com.joprelys.backend.medication.reference.MedicationReferenceService;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    void shouldHoldReferenceEquivalentActiveMedicationByIngredient() {
        AiMedicationSafetyGuard referenceGuard = guardWithReference();

        ParsedResponse result = referenceGuard.enforce(
                responseWithDrug("Tylenol"),
                Map.of("activeMedications", List.of(Map.of(
                        "drugName", "Acetaminophen",
                        "dosage", "500 mg"))),
                "en");

        assertTrue(result.needsClarification());
        assertEquals("prescription", result.clarification().field());
        assertTrue(result.assistantMessage().contains("Acetaminophen"));
        assertTrue(result.assistantMessage().contains("medication reference"));
        assertFalse(result.assistantMessage().contains("209459"));
        assertFalse(result.changes().stream().anyMatch(change -> "prescription".equals(change.field())));
    }

    @Test
    void shouldHoldReferenceEquivalentDocumentedAllergy() {
        AiMedicationSafetyGuard referenceGuard = guardWithReference();

        ParsedResponse result = referenceGuard.enforce(
                responseWithDrug("Tylenol"),
                Map.of("patient", Map.of("allergies", "Acetaminophen; latex")),
                "fr");

        assertTrue(result.needsClarification());
        assertTrue(result.assistantMessage().contains("Acetaminophen"));
        assertTrue(result.assistantMessage().contains("allergie documentée"));
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

    @Test
    void shouldRemainNonBlockingWhenReferenceIsUnavailable() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationReferencePort unavailable = new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                throw new IllegalStateException("network unavailable");
            }
        };
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(unavailable));
        AiMedicationSafetyGuard referenceGuard = new AiMedicationSafetyGuard(
                new ObjectMapper(), new MedicationReferenceDuplicateDetector(service));
        ParsedResponse original = responseWithDrug("Tylenol");

        ParsedResponse result = referenceGuard.enforce(
                original,
                Map.of("activeMedications", List.of(Map.of("drugName", "Acetaminophen"))),
                "fr");

        assertEquals(original, result);
    }

    private AiMedicationSafetyGuard guardWithReference() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationConcept branded = new MedicationConcept(
                "RXNORM", "209459", "acetaminophen tablet [Tylenol]", "SBD", List.of(), true);
        MedicationConcept ingredient = new MedicationConcept(
                "RXNORM", "161", "acetaminophen", "IN", List.of(), true);
        MedicationReferencePort provider = new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                return switch (query) {
                    case "Tylenol" -> List.of(branded);
                    case "Acetaminophen" -> List.of(ingredient);
                    default -> List.of();
                };
            }

            @Override
            public Set<String> findIngredientConceptIds(MedicationConcept concept) {
                return switch (concept.conceptId()) {
                    case "209459", "161" -> Set.of("161");
                    default -> Set.of();
                };
            }
        };
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(provider));
        return new AiMedicationSafetyGuard(
                new ObjectMapper(), new MedicationReferenceDuplicateDetector(service));
    }

    private MedicationReferenceProperties enabledProperties() {
        MedicationReferenceProperties properties = new MedicationReferenceProperties();
        properties.setEnabled(true);
        properties.setCacheTtlMinutes(60);
        properties.setMaxCandidates(5);
        return properties;
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
