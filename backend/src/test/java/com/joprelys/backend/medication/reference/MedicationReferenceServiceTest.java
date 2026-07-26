package com.joprelys.backend.medication.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MedicationReferenceServiceTest {

    @Test
    void shouldRemainDisabledByDefault() {
        MedicationReferenceProperties properties = new MedicationReferenceProperties();
        MedicationReferenceService service = new MedicationReferenceService(properties, List.of());

        MedicationReferenceLookup result = service.lookup("Paracétamol");

        assertFalse(result.resolved());
        assertFalse(result.degraded());
        assertEquals("MEDICATION_REFERENCE_DISABLED", result.warning());
    }

    @Test
    void shouldCacheEquivalentNormalizedQueries() {
        MedicationReferenceProperties properties = enabledProperties();
        AtomicInteger calls = new AtomicInteger();
        MedicationReferencePort provider = provider("RXNORM", 10, query -> {
            calls.incrementAndGet();
            return List.of(concept("161", "Acetaminophen"));
        });
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(provider));

        MedicationReferenceLookup first = service.lookup("Paracétamol");
        MedicationReferenceLookup second = service.lookup("  paracetamol  ");

        assertTrue(first.resolved());
        assertTrue(second.resolved());
        assertEquals(1, calls.get());
    }

    @Test
    void shouldUseNextProviderWhenFirstSourceFails() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationReferencePort failing = provider("PRIMARY", 1, query -> {
            throw new IllegalStateException("source unavailable");
        });
        MedicationReferencePort fallback = provider("FALLBACK", 2, query ->
                List.of(concept("A1", "Amoxicillin")));
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(fallback, failing));

        MedicationReferenceLookup result = service.lookup("Amoxicilline");

        assertTrue(result.resolved());
        assertFalse(result.degraded());
        assertEquals("FALLBACK", result.source());
    }

    @Test
    void shouldReportDegradedLookupWithoutBlocking() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationReferencePort failing = provider("RXNORM", 10, query -> {
            throw new IllegalStateException("network unavailable");
        });
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(failing));

        MedicationReferenceLookup result = service.lookup("Amlodipine");

        assertFalse(result.resolved());
        assertTrue(result.degraded());
        assertEquals("MEDICATION_REFERENCE_UNAVAILABLE", result.warning());
    }

    @Test
    void shouldConfirmOnlyPositiveSharedConceptEvidence() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationReferencePort provider = provider("RXNORM", 10, query -> switch (query) {
            case "Doliprane", "Paracétamol" -> List.of(concept("161", "Acetaminophen"));
            default -> List.of(concept("999", query));
        });
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(provider));

        MedicationEquivalence same = service.compare("Doliprane", "Paracétamol");
        MedicationEquivalence unknown = service.compare("Doliprane", "Ibuprofène");

        assertEquals(MedicationEquivalence.Status.SAME_CONCEPT, same.status());
        assertEquals("161", same.leftConceptId());
        assertEquals(MedicationEquivalence.Status.UNKNOWN, unknown.status());
    }

    @Test
    void shouldConfirmSameIngredientWhenProductConceptIdsDiffer() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationConcept branded = new MedicationConcept(
                "RXNORM", "209459", "acetaminophen 500 MG Oral Tablet [Tylenol]", "SBD", List.of(), true);
        MedicationConcept ingredient = new MedicationConcept(
                "RXNORM", "161", "acetaminophen", "IN", List.of(), true);
        MedicationReferencePort provider = new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public int priority() {
                return 10;
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

        MedicationEquivalence result = service.compare("Tylenol", "Acetaminophen");

        assertEquals(MedicationEquivalence.Status.SAME_INGREDIENT, result.status());
        assertEquals("RXNORM", result.source());
        assertEquals("209459", result.leftConceptId());
        assertEquals("161", result.rightConceptId());
        assertEquals("MEDICATION_SHARED_ACTIVE_INGREDIENT", result.explanation());
    }

    @Test
    void shouldRemainUnknownWhenIngredientSetsDoNotOverlap() {
        MedicationReferenceProperties properties = enabledProperties();
        MedicationConcept acetaminophenProduct = new MedicationConcept(
                "RXNORM", "209459", "acetaminophen tablet", "SCD", List.of(), true);
        MedicationConcept ibuprofenProduct = new MedicationConcept(
                "RXNORM", "5640P", "ibuprofen tablet", "SCD", List.of(), true);
        MedicationReferencePort provider = new MedicationReferencePort() {
            @Override
            public String source() {
                return "RXNORM";
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                return "Acetaminophen product".equals(query)
                        ? List.of(acetaminophenProduct)
                        : List.of(ibuprofenProduct);
            }

            @Override
            public Set<String> findIngredientConceptIds(MedicationConcept concept) {
                return "209459".equals(concept.conceptId()) ? Set.of("161") : Set.of("5640");
            }
        };
        MedicationReferenceService service = new MedicationReferenceService(
                properties, List.of(provider));

        MedicationEquivalence result = service.compare(
                "Acetaminophen product", "Ibuprofen product");

        assertEquals(MedicationEquivalence.Status.UNKNOWN, result.status());
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

    private MedicationReferencePort provider(
            String source,
            int priority,
            Lookup lookup) {
        return new MedicationReferencePort() {
            @Override
            public String source() {
                return source;
            }

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public List<MedicationConcept> findCandidates(String query, int maximum) {
                return lookup.find(query);
            }
        };
    }

    @FunctionalInterface
    private interface Lookup {
        List<MedicationConcept> find(String query);
    }
}
