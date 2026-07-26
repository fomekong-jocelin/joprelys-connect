package com.joprelys.backend.ai.medication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MedicationSafetyEngineTest {

    @Test
    void shouldDetectSameIngredientAcrossDifferentMedicationNames() {
        StubKnowledgeProvider knowledge = new StubKnowledgeProvider();
        knowledge.put("Augmentin", medication(
                "1001", "amoxicillin/clavulanate", ingredient("723", "amoxicillin"), ingredient("19711", "clavulanate")));
        knowledge.put("Amoxicilline", medication(
                "723", "amoxicillin", ingredient("723", "amoxicillin")));
        MedicationSafetyEngine engine = engine(knowledge, true);

        var assessment = engine.assess(
                "Augmentin",
                Map.of("activeMedications", List.of(Map.of("drugName", "Amoxicilline"))));

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.ACTIVE_DUPLICATE_INGREDIENT, assessment.findingType());
        assertEquals("amoxicillin", assessment.relatedMedicationOrFact());
    }

    @Test
    void shouldDetectAllergyThroughSharedResolvedIngredient() {
        StubKnowledgeProvider knowledge = new StubKnowledgeProvider();
        knowledge.put("Clamoxyl", medication(
                "2001", "amoxicillin product", ingredient("723", "amoxicillin")));
        knowledge.put("Amoxicilline", medication(
                "723", "amoxicillin", ingredient("723", "amoxicillin")));
        MedicationSafetyEngine engine = engine(knowledge, true);

        var assessment = engine.assess(
                "Clamoxyl",
                Map.of("patient", Map.of("allergies", "Amoxicilline; latex")));

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.DOCUMENTED_ALLERGY_INGREDIENT, assessment.findingType());
        assertEquals("Amoxicilline", assessment.relatedMedicationOrFact());
    }

    @Test
    void shouldKeepExactChecksLocalAndIndependentFromReferential() {
        MedicationKnowledgeProvider unavailable = drugName -> {
            throw new AssertionError("The referential must not be called for an exact duplicate");
        };
        MedicationSafetyEngine engine = engine(unavailable, true);

        var assessment = engine.assess(
                "Amlodipine",
                Map.of("activeMedications", List.of(Map.of("drugName", "amlodipine"))));

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.ACTIVE_DUPLICATE_EXACT, assessment.findingType());
        assertEquals("LOCAL", assessment.source());
    }

    @Test
    void shouldHoldUnresolvedMedicationWhenFailClosed() {
        MedicationSafetyEngine engine = engine(drugName -> Optional.empty(), true);

        var assessment = engine.assess("MarqueLocaleInconnue", Map.of());

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.MEDICATION_UNRESOLVED, assessment.findingType());
    }

    @Test
    void shouldAllowUnresolvedMedicationWhenFailClosedIsDisabled() {
        MedicationSafetyEngine engine = engine(drugName -> Optional.empty(), false);

        var assessment = engine.assess("MarqueLocaleInconnue", Map.of());

        assertFalse(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.NONE, assessment.findingType());
    }

    @Test
    void shouldHoldPrescriptionWhenReferentialIsUnavailableAndFailClosed() {
        MedicationKnowledgeProvider unavailable = drugName -> {
            throw new MedicationKnowledgeUnavailableException("offline", new IllegalStateException());
        };
        MedicationSafetyEngine engine = engine(unavailable, true);

        var assessment = engine.assess("Amoxicilline", Map.of());

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.REFERENTIAL_UNAVAILABLE, assessment.findingType());
    }

    @Test
    void shouldNotUseSharedAtcClassAloneAsClinicalConflict() {
        StubKnowledgeProvider knowledge = new StubKnowledgeProvider();
        var sameClass = List.of(new MedicationKnowledgeProvider.MedicationClass(
                "ATC", "C09AA", "ACE inhibitors, plain", "ATC4", null));
        knowledge.put("Drug A", new MedicationKnowledgeProvider.MedicationKnowledge(
                "RXNORM", "1", "drug a", List.of(ingredient("11", "ingredient a")), sameClass));
        knowledge.put("Drug B", new MedicationKnowledgeProvider.MedicationKnowledge(
                "RXNORM", "2", "drug b", List.of(ingredient("22", "ingredient b")), sameClass));
        MedicationSafetyEngine engine = engine(knowledge, true);

        var assessment = engine.assess(
                "Drug A",
                Map.of("activeMedications", List.of(Map.of("drugName", "Drug B"))));

        assertFalse(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.NONE, assessment.findingType());
    }

    @Test
    void shouldSurfaceAuthoritativeInteractionWhenProviderReportsOne() {
        StubKnowledgeProvider knowledge = new StubKnowledgeProvider();
        knowledge.put("Drug A", medication("1", "drug a", ingredient("11", "ingredient a")));
        knowledge.put("Drug B", medication("2", "drug b", ingredient("22", "ingredient b")));
        MedicationInteractionProvider interactions = medications -> new MedicationInteractionProvider.InteractionResult(
                MedicationInteractionProvider.Status.CHECKED,
                List.of(new MedicationInteractionProvider.MedicationInteraction(
                        "1", "2", "MAJOR", "Potential major interaction", "evidence-1")),
                "TEST-AUTHORITATIVE");
        MedicationSafetyEngine engine = new MedicationSafetyEngine(knowledge, interactions, properties(true));

        var assessment = engine.assess(
                "Drug A",
                Map.of("activeMedications", List.of(Map.of("drugName", "Drug B"))));

        assertTrue(assessment.requiresConfirmation());
        assertEquals(MedicationSafetyEngine.FindingType.DRUG_INTERACTION, assessment.findingType());
        assertEquals("TEST-AUTHORITATIVE", assessment.source());
    }

    private MedicationSafetyEngine engine(
            MedicationKnowledgeProvider knowledge,
            boolean failClosed) {
        MedicationInteractionProvider noInteractions = medications ->
                MedicationInteractionProvider.InteractionResult.unavailable("NONE");
        return new MedicationSafetyEngine(knowledge, noInteractions, properties(failClosed));
    }

    private MedicationSafetyProperties properties(boolean failClosed) {
        return new MedicationSafetyProperties(
                true,
                failClosed,
                "rxnorm",
                "none",
                new MedicationSafetyProperties.RxNormProperties(
                        "https://rxnav.nlm.nih.gov/REST", 1000, 1000, 60));
    }

    private MedicationKnowledgeProvider.MedicationKnowledge medication(
            String id,
            String name,
            MedicationKnowledgeProvider.MedicationIngredient... ingredients) {
        return new MedicationKnowledgeProvider.MedicationKnowledge(
                "RXNORM", id, name, List.of(ingredients), List.of());
    }

    private MedicationKnowledgeProvider.MedicationIngredient ingredient(String id, String name) {
        return new MedicationKnowledgeProvider.MedicationIngredient(id, name, "IN");
    }

    private static final class StubKnowledgeProvider implements MedicationKnowledgeProvider {
        private final Map<String, MedicationKnowledge> values = new HashMap<>();

        void put(String name, MedicationKnowledge knowledge) {
            values.put(name.toLowerCase(), knowledge);
        }

        @Override
        public Optional<MedicationKnowledge> resolve(String drugName) {
            return Optional.ofNullable(values.get(drugName.toLowerCase()));
        }
    }
}
