package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiConsultationContinuousCaptureTest {

    private AiProvider aiProvider;
    private ClinicalContextAssembler clinicalContextAssembler;
    private AiConsultationService service;
    private UUID visitId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        VisitService visitService = mock(VisitService.class);
        clinicalContextAssembler = mock(ClinicalContextAssembler.class);
        VisitEntity visit = mock(VisitEntity.class);
        visitId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(clinicalContextAssembler.assemble(visitId)).thenReturn(Map.of());

        AiProperties properties = new AiProperties(
                true,
                "openai",
                "openai",
                30,
                20,
                0.35,
                "fr",
                null,
                null,
                null);
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AiConsultationService(
                aiProvider,
                properties,
                visitService,
                objectMapper,
                new AiClinicalResponseParser(objectMapper),
                clinicalContextAssembler,
                new AiRevisionManager(),
                new AiClarificationManager(properties));
        service.startSession(visitId, userId, organizationId, Map.of());
    }

    @Test
    void continuousCaptureMustNotLeaveClarificationPendingOrBlockNextPhrase() {
        when(aiProvider.chat(anyList(), anyString())).thenReturn(
                chatResponse("""
                        {
                          "changes": [],
                          "assistantMessage": "Pouvez-vous préciser ?",
                          "needsClarification": true,
                          "clarification": {
                            "field": "clinicalExam",
                            "question": "Pouvez-vous préciser ?",
                            "options": []
                          }
                        }
                        """),
                chatResponse("""
                        {
                          "changes": [{
                            "field": "symptoms",
                            "operation": "SET",
                            "value": "Céphalées sévères",
                            "reason": "Symptôme explicitement dicté.",
                            "uncertainty": "LOW",
                            "evidence": ["céphalées sévères"]
                          }],
                          "assistantMessage": "Élément structuré.",
                          "needsClarification": false,
                          "clarification": null
                        }
                        """));

        var first = service.processRealtimeTranscript(
                visitId, userId, organizationId, "Je voudrais savoir ce que vous avez observé.", 0.7);
        var second = service.processRealtimeTranscript(
                visitId, userId, organizationId, "Le patient présente des céphalées sévères.", 0.92);

        assertFalse(first.needsClarification());
        assertTrue(first.clarifications().isEmpty());
        assertFalse(second.needsClarification());
        assertEquals("Céphalées sévères", second.draft().get("symptoms"));
        assertTrue(second.revisions().stream().allMatch(revision -> "DECIDED".equals(revision.status())));
        verify(aiProvider, times(2)).chat(anyList(), anyString());
    }

    @Test
    void continuousCaptureMustKeepSafeFactsEvenWhenAnotherFieldNeedsClarification() {
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [{
                    "field": "symptoms",
                    "operation": "SET",
                    "value": "Céphalée aiguë depuis trois jours",
                    "reason": "Symptôme explicite.",
                    "uncertainty": "LOW",
                    "evidence": ["céphalée aiguë depuis trois jours"]
                  }],
                  "assistantMessage": "Précision médicament nécessaire.",
                  "needsClarification": true,
                  "clarification": {
                    "field": "prescription",
                    "question": "Pouvez-vous préciser la fréquence ?",
                    "options": []
                  }
                }
                """));

        var result = service.processRealtimeTranscript(
                visitId,
                userId,
                organizationId,
                "Céphalée aiguë depuis trois jours. Vitafer une fois pas jour.",
                0.9);

        assertEquals("Céphalée aiguë depuis trois jours", result.draft().get("symptoms"));
        assertFalse(result.needsClarification());
        assertTrue(result.clarifications().isEmpty());
    }

    @Test
    void rebuildWithoutAsrConfidenceMustRemainStructurableAfterDurableReview() {
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [{
                    "field": "symptoms",
                    "operation": "SET",
                    "value": "Douleur du bras",
                    "reason": "Symptôme explicitement dicté.",
                    "uncertainty": "MEDIUM",
                    "evidence": ["douleur du bras"]
                  }],
                  "assistantMessage": "Élément structuré.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        var result = service.processRealtimeTranscript(
                visitId, userId, organizationId, "Le patient signale une douleur du bras.", null);

        assertEquals("Douleur du bras", result.draft().get("symptoms"));
        assertFalse(result.needsClarification());
    }

    @Test
    void phantomPrescriptionMustBeDroppedWhenNoMedicationWasDictated() {
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Céphalées sévères",
                      "reason": "Symptôme explicite.",
                      "uncertainty": "LOW",
                      "evidence": ["céphalées sévères"]
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Amoxicilline","dosage":"500 mg"}],
                      "reason": "Prescription proposée par le modèle.",
                      "uncertainty": "LOW",
                      "evidence": ["Amoxicilline"]
                    }
                  ],
                  "assistantMessage": "Compte rendu structuré.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        var result = service.processRealtimeTranscript(
                visitId, userId, organizationId, "Le patient présente des céphalées sévères.", 0.95);

        assertEquals("Céphalées sévères", result.draft().get("symptoms"));
        assertFalse(result.draft().containsKey("prescription"));
    }

    @Test
    void explicitMedicationMayReachWorkingDraftOnlyWhenGroundedInClinicianWords() {
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [{
                    "field": "prescription",
                    "operation": "SET",
                    "value": [{"drugName":"Paracétamol","dosage":"1000 mg","frequency":"matin et soir","duration":"4 jours"}],
                    "reason": "Prescription explicitement dictée.",
                    "uncertainty": "LOW",
                    "evidence": ["Paracétamol 1000 mg", "matin et soir", "4 jours"]
                  }],
                  "assistantMessage": "Prescription structurée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        var result = service.processRealtimeTranscript(
                visitId,
                userId,
                organizationId,
                "Je prescris du Paracétamol 1000 mg matin et soir pendant 4 jours.",
                0.97);

        assertTrue(result.draft().get("prescription").contains("Paracétamol"));
        assertTrue(result.draft().get("prescription").contains("1000 mg"));
    }

    @Test
    void durableCaptureMustStructureTheMessyRecetteTranscriptInOneModelCall() {
        String transcript = """
                Je reçois aujourd'hui un patient âgé de 12 ans qui se plaint d'une céphalée aiguë depuis trois jours.
                Il n'arrive plus à se lever. Quatre jours, je lui demande également de beaucoup boire et de se reposer.
                Je lui prescrit du Paracétamol 1000mg a prendre matin midi soir 2 comprimé par prise par voie orale
                Vitafer 1cuillerr a café 1 fois pas jour voie orale Examen goutte d'epaisse
                """.trim();
        when(aiProvider.chatStructured(anyList(), anyString(), anyString(), anyMap())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "céphalée aiguë depuis trois jours. Il n'arrive plus à se lever",
                      "reason": "Symptômes explicitement dictés.",
                      "uncertainty": "LOW",
                      "evidence": ["céphalée aiguë depuis trois jours", "Il n'arrive plus à se lever"]
                    },
                    {
                      "field": "advice",
                      "operation": "SET",
                      "value": "beaucoup boire et se reposer",
                      "reason": "Conseils explicitement dictés.",
                      "uncertainty": "LOW",
                      "evidence": ["beaucoup boire et de se reposer"]
                    },
                    {
                      "field": "prescription",
                      "operation": "SET",
                      "value": [{"drugName":"Paracétamol","dosage":"1000 mg","frequency":"matin midi soir","posology":"2 comprimé par prise","route":"voie orale"},{"drugName":"Vitafer","instructions":"1cuillerr a café","frequency":"1 fois pas jour","route":"voie orale"}],
                      "reason": "Prescription explicitement dictée.",
                      "uncertainty": "MEDIUM",
                      "evidence": ["Paracétamol 1000mg", "2 comprimé par prise", "Vitafer", "1 fois pas jour voie orale"]
                    },
                    {
                      "field": "labOrders",
                      "operation": "SET",
                      "value": ["goutte épaisse"],
                      "reason": "Examen explicitement demandé.",
                      "uncertainty": "LOW",
                      "evidence": ["Examen goutte d'epaisse"]
                    }
                  ],
                  "assistantMessage": "Capture structurée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        var result = service.processCaptureTranscript(visitId, userId, organizationId, transcript);

        assertTrue(result.draft().get("symptoms").contains("céphalée aiguë depuis trois jours"));
        assertTrue(result.draft().get("symptoms").contains("n'arrive plus à se lever"));
        assertTrue(result.draft().get("advice").contains("boire"));
        assertTrue(result.draft().get("prescription").contains("Paracétamol"));
        assertTrue(result.draft().get("prescription").contains("1000 mg"));
        assertTrue(result.draft().get("prescription").contains("Vitafer"));
        assertTrue(result.draft().get("labOrders").contains("goutte épaisse"));
        assertFalse(result.draft().containsKey("clinicalExam"));
        assertFalse(result.draft().containsKey("diagnosis"));
        verify(aiProvider, times(1)).chatStructured(anyList(), anyString(), anyString(), anyMap());
    }

    private AiChatResponse chatResponse(String content) {
        return new AiChatResponse(content, 80, "gpt-4o-mini");
    }
}
