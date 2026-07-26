package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AiConsultationSafetyIntegrationTest {

    private AiProvider provider;
    private ClinicalContextAssembler contextAssembler;
    private AiConsultationService service;
    private UUID visitId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        provider = mock(AiProvider.class);
        VisitService visitService = mock(VisitService.class);
        contextAssembler = mock(ClinicalContextAssembler.class);
        VisitEntity visit = mock(VisitEntity.class);
        visitId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of());

        AiProperties properties = new AiProperties(
                true, "openai", "openai", 30, 20, "fr", null, null, null);
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AiConsultationService(
                provider,
                properties,
                visitService,
                objectMapper,
                new AiClinicalResponseParser(objectMapper),
                contextAssembler,
                new AiRevisionManager(),
                new AiClarificationManager(properties));
        service.startSession(visitId, userId, organizationId, Map.of(), "fr");
    }

    @Test
    void shouldFailClosedWhenClinicalContextCannotBeLoaded() {
        when(contextAssembler.assemble(visitId)).thenThrow(new IllegalStateException("database unavailable"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processText(visitId, userId, organizationId, "Le patient a de la fièvre."));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("AI_CLINICAL_CONTEXT_UNAVAILABLE", exception.getReason());
    }

    @Test
    void shouldAskClinicianBeforeExactDuplicateActiveMedication() {
        when(contextAssembler.assemble(visitId)).thenReturn(Map.of(
                "activeMedications", List.of(Map.of("drugName", "Amlodipine", "dosage", "5 mg"))));
        when(provider.chat(anyList(), anyString())).thenReturn(responseWithPrescription("Amlodipine"));

        var response = service.processText(
                visitId,
                userId,
                organizationId,
                "Je prescris Amlodipine cinq milligrammes une fois par jour.");

        assertTrue(response.needsClarification());
        assertTrue(response.revisions().isEmpty());
        assertEquals("prescription", response.clarifications().getLast().field());
        assertTrue(response.assistantMessage().contains("traitements actifs"));
    }

    @Test
    void shouldRejectDuplicateActionsOnSameFieldBeforeRevisionCreation() {
        when(provider.chat(anyList(), anyString())).thenReturn(new AiChatResponse("""
                {
                  "changes": [
                    {"field":"symptoms","operation":"SET","value":"Fièvre","reason":"dicté","uncertainty":"LOW"},
                    {"field":"symptoms","operation":"SET","value":"Fièvre et céphalées","reason":"dicté","uncertainty":"LOW"}
                  ],
                  "assistantMessage":"Deux versions.",
                  "needsClarification":false,
                  "clarification":null
                }
                """, 100, "test"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processText(
                        visitId, userId, organizationId, "Fièvre avec céphalées."));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("AI_TOOL_DUPLICATE_FIELD", exception.getReason());
        assertFalse(service.getSession(visitId, userId, organizationId)
                .orElseThrow().revisions().stream().anyMatch(revision -> "PENDING".equals(revision.status())));
    }

    private AiChatResponse responseWithPrescription(String drugName) {
        return new AiChatResponse("""
                {
                  "changes": [{
                    "field":"prescription",
                    "operation":"SET",
                    "value":[{"drugName":"%s","dosage":"5 mg","frequency":"1 fois par jour"}],
                    "reason":"Prescription explicitement dictée.",
                    "uncertainty":"LOW"
                  }],
                  "assistantMessage":"Prescription proposée.",
                  "needsClarification":false,
                  "clarification":null
                }
                """.formatted(drugName), 100, "test");
    }
}
