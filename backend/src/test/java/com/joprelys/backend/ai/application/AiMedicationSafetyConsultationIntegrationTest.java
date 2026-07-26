package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.ai.medication.MedicationInteractionProvider;
import com.joprelys.backend.ai.medication.MedicationKnowledgeProvider;
import com.joprelys.backend.ai.medication.MedicationSafetyEngine;
import com.joprelys.backend.ai.medication.MedicationSafetyProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiMedicationSafetyConsultationIntegrationTest {

    @Test
    void shouldAskOnceForIngredientDuplicateThenAllowExplicitClinicianConfirmation() {
        AiProvider provider = mock(AiProvider.class);
        VisitService visitService = mock(VisitService.class);
        ClinicalContextAssembler contextAssembler = mock(ClinicalContextAssembler.class);
        VisitEntity visit = mock(VisitEntity.class);
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of(
                "activeMedications", List.of(Map.of("drugName", "Amoxicilline"))));
        when(provider.chat(anyList(), anyString())).thenReturn(
                prescriptionResponse("Augmentin"),
                prescriptionResponse("Augmentin"));

        MedicationKnowledgeProvider knowledge = drugName -> switch (drugName.toLowerCase()) {
            case "augmentin" -> Optional.of(medication(
                    "1001", "amoxicillin / clavulanate", List.of(
                            ingredient("723", "amoxicillin"),
                            ingredient("19711", "clavulanate"))));
            case "amoxicilline" -> Optional.of(medication(
                    "723", "amoxicillin", List.of(ingredient("723", "amoxicillin"))));
            default -> Optional.empty();
        };
        MedicationInteractionProvider noInteractions = medications ->
                MedicationInteractionProvider.InteractionResult.unavailable("NONE");
        MedicationSafetyEngine safetyEngine = new MedicationSafetyEngine(
                knowledge, noInteractions, medicationSafetyProperties());

        AiProperties aiProperties = new AiProperties(
                true, "openai", "openai", 30, 20, "fr", null, null, null);
        ObjectMapper objectMapper = new ObjectMapper();
        AiConsultationService service = new AiConsultationService(
                provider,
                aiProperties,
                visitService,
                objectMapper,
                new AiClinicalResponseParser(objectMapper),
                contextAssembler,
                new AiRevisionManager(),
                new AiClarificationManager(aiProperties),
                Optional.of(safetyEngine));
        service.startSession(visitId, userId, organizationId, Map.of(), "fr");

        var first = service.processText(
                visitId,
                userId,
                organizationId,
                "Je prescris Augmentin 500 milligrammes.");

        assertTrue(first.needsClarification());
        assertTrue(first.revisions().isEmpty());
        assertEquals("prescription", first.clarifications().getLast().field());
        assertTrue(first.assistantMessage().contains("principe actif"));

        var confirmed = service.answerClarification(
                visitId,
                userId,
                organizationId,
                first.clarifications().getLast().id(),
                "Oui, je confirme Augmentin.");

        assertFalse(confirmed.needsClarification());
        assertEquals("RESOLVED", confirmed.clarifications().getLast().status());
        assertEquals(1, confirmed.revisions().size());
        assertEquals("prescription", confirmed.revisions().getLast().proposals().getFirst().field());
    }

    private AiChatResponse prescriptionResponse(String drugName) {
        return new AiChatResponse("""
                {
                  "changes": [{
                    "field":"prescription",
                    "operation":"SET",
                    "value":[{"drugName":"%s","dosage":"500 mg"}],
                    "reason":"Prescription explicitement dictée.",
                    "uncertainty":"LOW"
                  }],
                  "assistantMessage":"Prescription proposée.",
                  "needsClarification":false,
                  "clarification":null
                }
                """.formatted(drugName), 100, "test");
    }

    private MedicationKnowledgeProvider.MedicationKnowledge medication(
            String id,
            String name,
            List<MedicationKnowledgeProvider.MedicationIngredient> ingredients) {
        return new MedicationKnowledgeProvider.MedicationKnowledge(
                "RXNORM", id, name, ingredients, List.of());
    }

    private MedicationKnowledgeProvider.MedicationIngredient ingredient(String id, String name) {
        return new MedicationKnowledgeProvider.MedicationIngredient(id, name, "IN");
    }

    private MedicationSafetyProperties medicationSafetyProperties() {
        return new MedicationSafetyProperties(
                true,
                true,
                "rxnorm",
                "none",
                new MedicationSafetyProperties.RxNormProperties(
                        "https://rxnav.nlm.nih.gov/REST", 1000, 1000, 60));
    }
}
