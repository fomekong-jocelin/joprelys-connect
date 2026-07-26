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
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiPrescriptionRegressionTest {

    @Test
    void shouldNeverCreatePrescriptionWhenLatestDictationContainsNoMedication() {
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
                "patient", Map.of("allergies", "Pénicilline")));
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Toux sèche depuis trois jours",
                      "reason": "Symptôme dicté.",
                      "uncertainty": "LOW"
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Paracétamol","dosage":"1 g"}],
                      "reason": "Traitement proposé par le modèle.",
                      "uncertainty": "LOW"
                    }
                  ],
                  "assistantMessage": "J'ai structuré la consultation.",
                  "needsClarification": false,
                  "clarification": null
                }
                """, 100, "test-model"));

        AiProperties properties = new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                "fr",
                null,
                null,
                null);
        ObjectMapper objectMapper = new ObjectMapper();
        AiConsultationService service = new AiConsultationService(
                provider,
                properties,
                visitService,
                objectMapper,
                new AiClinicalResponseParser(objectMapper),
                contextAssembler,
                new AiRevisionManager(),
                new AiClarificationManager(properties));

        service.startSession(
                visitId,
                userId,
                organizationId,
                Map.of(
                        "prescription",
                        "[{\"drugName\":\"Amoxicilline\",\"dosage\":\"500 mg\"}]"),
                "fr");

        var response = service.processText(
                visitId,
                userId,
                organizationId,
                "Le patient présente une toux sèche depuis trois jours.");

        assertEquals(1, response.revisions().size());
        assertEquals(1, response.revisions().getFirst().proposals().size());
        assertEquals("symptoms", response.revisions().getFirst().proposals().getFirst().field());
        assertFalse(response.changedFields().contains("prescription"));
        assertTrue(response.assistantMessage().contains("Aucun médicament"));
    }
}
