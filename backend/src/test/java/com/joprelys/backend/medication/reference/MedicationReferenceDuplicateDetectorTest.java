package com.joprelys.backend.medication.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MedicationReferenceDuplicateDetectorTest {

    @Test
    void shouldReturnPositiveEvidenceForSharedIngredient() {
        MedicationConcept branded = new MedicationConcept(
                "RXNORM", "209459", "acetaminophen tablet [Tylenol]", "SBD", List.of(), true);
        MedicationConcept ingredient = new MedicationConcept(
                "RXNORM", "161", "acetaminophen", "IN", List.of(), true);
        MedicationReferenceDuplicateDetector detector = detector(new MedicationReferencePort() {
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
                return Set.of("161");
            }
        });

        MedicationReferenceMatch match = detector.findEquivalent(
                        "Tylenol", List.of("Ibuprofen", "Acetaminophen"))
                .orElseThrow();

        assertEquals("Acetaminophen", match.matchedName());
        assertEquals(MedicationEquivalence.Status.SAME_INGREDIENT, match.status());
        assertEquals("RXNORM", match.source());
        assertEquals("209459", match.proposedConceptId());
        assertEquals("161", match.matchedConceptId());
    }

    @Test
    void shouldRemainEmptyWhenNoPositiveReferenceEvidenceExists() {
        MedicationReferenceDuplicateDetector detector = detector(new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                return switch (query) {
                    case "Acetaminophen" -> List.of(new MedicationConcept(
                            "RXNORM", "161", "acetaminophen", "IN", List.of(), true));
                    case "Ibuprofen" -> List.of(new MedicationConcept(
                            "RXNORM", "5640", "ibuprofen", "IN", List.of(), true));
                    default -> List.of();
                };
            }
        });

        assertTrue(detector.findEquivalent(
                "Acetaminophen", List.of("Ibuprofen")).isEmpty());
    }

    @Test
    void shouldRemainEmptyWhenReferenceProviderIsUnavailable() {
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
        MedicationReferenceDuplicateDetector detector = detector(unavailable);

        assertTrue(detector.findEquivalent(
                "Tylenol", List.of("Acetaminophen")).isEmpty());
    }

    private MedicationReferenceDuplicateDetector detector(MedicationReferencePort provider) {
        MedicationReferenceProperties properties = new MedicationReferenceProperties();
        properties.setEnabled(true);
        properties.setCacheTtlMinutes(60);
        properties.setMaxCandidates(5);
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(provider));
        return new MedicationReferenceDuplicateDetector(service);
    }
}
