package com.joprelys.backend.medication.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MedicationReferenceDuplicateDetectorTest {

    @Test
    void shouldReturnPositiveEvidenceForBrandAndIngredientSharingAConcept() {
        MedicationReferenceDuplicateDetector detector = detector(query -> switch (query) {
            case "Doliprane", "Paracétamol" -> List.of(concept("161", "Acetaminophen"));
            default -> List.of();
        });

        var match = detector.findEquivalent(
                "Doliprane", List.of("Amlodipine", "Paracétamol"));

        assertTrue(match.isPresent());
        assertEquals("Paracétamol", match.orElseThrow().matchedName());
        assertEquals("RXNORM", match.orElseThrow().source());
        assertEquals("161", match.orElseThrow().conceptId());
    }

    @Test
    void shouldRemainEmptyWhenNoSharedConceptIsProven() {
        MedicationReferenceDuplicateDetector detector = detector(query -> switch (query) {
            case "Doliprane" -> List.of(concept("161", "Acetaminophen"));
            case "Ibuprofène" -> List.of(concept("5640", "Ibuprofen"));
            default -> List.of();
        });

        var match = detector.findEquivalent("Doliprane", List.of("Ibuprofène"));

        assertTrue(match.isEmpty());
    }

    @Test
    void shouldRemainEmptyWhenReferenceProviderIsUnavailable() {
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
        MedicationReferenceDuplicateDetector detector =
                new MedicationReferenceDuplicateDetector(service);

        var match = detector.findEquivalent("Doliprane", List.of("Paracétamol"));

        assertTrue(match.isEmpty());
    }

    private MedicationReferenceDuplicateDetector detector(Lookup lookup) {
        MedicationReferencePort provider = new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                return lookup.find(query);
            }
        };
        MedicationReferenceService service = new MedicationReferenceService(
                enabledProperties(), List.of(provider));
        return new MedicationReferenceDuplicateDetector(service);
    }

    private MedicationReferenceProperties enabledProperties() {
        MedicationReferenceProperties properties = new MedicationReferenceProperties();
        properties.setEnabled(true);
        properties.setCacheTtlMinutes(60);
        properties.setMaxCandidates(5);
        return properties;
    }

    private MedicationConcept concept(String id, String name) {
        return new MedicationConcept("RXNORM", id, name, "IN", List.of(), true);
    }

    @FunctionalInterface
    private interface Lookup {
        List<MedicationConcept> find(String query);
    }
}
