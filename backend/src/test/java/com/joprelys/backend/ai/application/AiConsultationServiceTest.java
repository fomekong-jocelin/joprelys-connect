package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AiConsultationServiceTest {

    private AiProvider aiProvider;
    private VisitService visitService;
    private VisitEntity visit;
    private AiConsultationService service;
    private UUID visitId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        visitService = mock(VisitService.class);
        visit = mock(VisitEntity.class);
        visitId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        when(visitService.getVisit(visitId)).thenReturn(visit);
        when(visit.getStatus()).thenReturn("EN_COURS");

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
        service = new AiConsultationService(
                aiProvider,
                properties,
                visitService,
                new ObjectMapper());
    }

    @Test
    void shouldCreateSessionAndMergeStructuredDraft() {
        service.startSession(
                visitId,
                userId,
                organizationId,
                Map.of("symptoms", "Fièvre depuis deux jours"));
        when(aiProvider.chat(anyList(), anyString())).thenReturn(new AiChatResponse(
                """
                {
                  "draft": {
                    "symptoms": "Fièvre depuis deux jours avec céphalées",
                    "clinicalExam": "Température à 38,5 °C",
                    "unknownField": "doit être ignoré"
                  },
                  "assistantMessage": "Précisez l'intensité des céphalées.",
                  "needsClarification": true
                }
                """,
                120,
                "gpt-4.1"));

        AiConsultationService.MessageView response = service.processText(
                visitId,
                userId,
                organizationId,
                "Le patient présente aussi des céphalées.");

        assertEquals(
                "Fièvre depuis deux jours avec céphalées",
                response.draft().get("symptoms"));
        assertEquals("Température à 38,5 °C", response.draft().get("clinicalExam"));
        assertFalse(response.draft().containsKey("unknownField"));
        assertTrue(response.changedFields().contains("symptoms"));
        assertTrue(response.needsClarification());
    }

    @Test
    void shouldTranscribeAudioBeforeUpdatingDraft() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.transcribeAudio(new byte[] {1, 2, 3}, "audio/webm", "fr"))
                .thenReturn(new AiTranscription("Toux sèche depuis trois jours", "fr", null));
        when(aiProvider.chat(anyList(), anyString())).thenReturn(new AiChatResponse(
                """
                {
                  "draft": {"symptoms": "Toux sèche depuis trois jours"},
                  "assistantMessage": "Brouillon mis à jour.",
                  "needsClarification": false
                }
                """,
                80,
                "gpt-4.1"));

        AiConsultationService.MessageView response = service.processAudio(
                visitId,
                userId,
                organizationId,
                new byte[] {1, 2, 3},
                "audio/webm;codecs=opus");

        assertEquals("Toux sèche depuis trois jours", response.transcript());
        assertEquals("Toux sèche depuis trois jours", response.draft().get("symptoms"));
    }

    @Test
    void shouldRejectUnsupportedAudioBeforeCallingProvider() {
        service.startSession(visitId, userId, organizationId, Map.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processAudio(
                        visitId,
                        userId,
                        organizationId,
                        new byte[] {1},
                        "audio/ogg"));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
        verify(aiProvider, never()).transcribeAudio(
                org.mockito.ArgumentMatchers.any(),
                anyString(),
                anyString());
    }

    @Test
    void shouldRejectSessionWhenVisitIsNoLongerActive() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(visit.getStatus()).thenReturn("TERMINEE");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getSession(visitId, userId, organizationId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
