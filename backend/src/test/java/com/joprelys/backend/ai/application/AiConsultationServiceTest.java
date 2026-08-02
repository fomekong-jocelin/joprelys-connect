package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.application.AiConsultationContract.MessageView;
import com.joprelys.backend.ai.application.AiConsultationContract.RevisionView;
import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.application.AiConsultationContract.TranscriptionView;
import com.joprelys.backend.ai.domain.AiChatResponse;
import com.joprelys.backend.ai.domain.AiMessage;
import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.AiProperties;
import com.joprelys.backend.visit.application.VisitService;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

class AiConsultationServiceTest {

    private AiProvider aiProvider;
    private VisitService visitService;
    private ClinicalContextAssembler clinicalContextAssembler;
    private VisitEntity visit;
    private AiConsultationService service;
    private UUID visitId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        aiProvider = mock(AiProvider.class);
        visitService = mock(VisitService.class);
        clinicalContextAssembler = mock(ClinicalContextAssembler.class);
        visit = mock(VisitEntity.class);
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
    }

    @Test
    void shouldCreateRevisionWithoutMutatingAcceptedDraft() {
        service.startSession(
                visitId,
                userId,
                organizationId,
                Map.of("symptoms", "Fièvre depuis deux jours"));
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Fièvre depuis deux jours avec céphalées",
                      "reason": "Le médecin ajoute des céphalées.",
                      "uncertainty": "LOW",
                      "evidence": ["céphalées"]
                    },
                    {
                      "field": "clinicalExam",
                      "operation": "SET",
                      "value": "Température à 38,5 °C",
                      "reason": "Température dictée.",
                      "uncertainty": "LOW",
                      "evidence": ["température à 38,5"]
                    }
                  ],
                  "assistantMessage": "Deux modifications sont proposées.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        MessageView response = service.processText(
                visitId,
                userId,
                organizationId,
                "Le patient présente aussi des céphalées et une température à 38,5.");

        assertEquals("Fièvre depuis deux jours", response.draft().get("symptoms"));
        assertFalse(response.draft().containsKey("clinicalExam"));
        assertEquals(1, response.revisions().size());
        RevisionView revision = response.revisions().getFirst();
        assertEquals("PENDING", revision.status());
        assertEquals(2, revision.proposals().size());
        assertTrue(response.changedFields().contains("symptoms"));
        assertEquals(3, response.conversation().size());
    }

    @Test
    void shouldAcceptOneProposalAndRejectAnother() {
        service.startSession(
                visitId, userId, organizationId, Map.of("symptoms", "Fièvre"));
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Fièvre avec céphalées",
                      "reason": "Symptôme ajouté.",
                      "uncertainty": "LOW",
                      "evidence": ["céphalées"]
                    },
                    {
                      "field": "clinicalExam",
                      "operation": "SET",
                      "value": "Température à 38,5 °C",
                      "reason": "Valeur dictée.",
                      "uncertainty": "LOW",
                      "evidence": ["température à 38,5"]
                    }
                  ],
                  "assistantMessage": "Vérifiez les modifications.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));
        MessageView response = service.processText(
                visitId,
                userId,
                organizationId,
                "Ajoute les céphalées et note une température à 38,5.");
        RevisionView revision = response.revisions().getFirst();

        SessionView afterAccept = service.decideProposal(
                visitId,
                userId,
                organizationId,
                revision.id(),
                revision.proposals().get(0).id(),
                "ACCEPT");
        assertEquals("Fièvre avec céphalées", afterAccept.draft().get("symptoms"));
        assertFalse(afterAccept.draft().containsKey("clinicalExam"));
        assertEquals("PENDING", afterAccept.revisions().getFirst().status());

        SessionView afterReject = service.decideProposal(
                visitId,
                userId,
                organizationId,
                revision.id(),
                revision.proposals().get(1).id(),
                "REJECT");
        assertFalse(afterReject.draft().containsKey("clinicalExam"));
        assertEquals("DECIDED", afterReject.revisions().getFirst().status());
        assertEquals("ACCEPTED", afterReject.revisions().getFirst().proposals().get(0).status());
        assertEquals("REJECTED", afterReject.revisions().getFirst().proposals().get(1).status());
    }

    @Test
    void shouldClearFieldOnlyAfterAcceptedClearProposal() {
        service.startSession(
                visitId,
                userId,
                organizationId,
                Map.of("diagnosis", "Diagnostic provisoire erroné"));
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "diagnosis",
                      "operation": "CLEAR",
                      "reason": "Le médecin demande de supprimer le diagnostic.",
                      "uncertainty": "LOW",
                      "evidence": ["Supprime le diagnostic provisoire"]
                    }
                  ],
                  "assistantMessage": "La suppression du diagnostic est proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        MessageView response = service.processText(
                visitId, userId, organizationId, "Supprime le diagnostic provisoire.");
        assertEquals("Diagnostic provisoire erroné", response.draft().get("diagnosis"));

        RevisionView revision = response.revisions().getFirst();
        SessionView decided = service.decideProposal(
                visitId,
                userId,
                organizationId,
                revision.id(),
                revision.proposals().getFirst().id(),
                "ACCEPT");

        assertFalse(decided.draft().containsKey("diagnosis"));
        assertEquals("ACCEPTED", decided.revisions().getFirst()
                .proposals().getFirst().status());
    }

    @Test
    void shouldDecideAllPendingProposalsAtOnce() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Toux sèche",
                      "reason": "Symptôme dicté.",
                      "uncertainty": "LOW",
                      "evidence": ["Toux sèche"]
                    },
                    {
                      "field": "clinicalExam",
                      "operation": "SET",
                      "value": "Auscultation normale",
                      "reason": "Examen dicté.",
                      "uncertainty": "LOW",
                      "evidence": ["auscultation normale"]
                    }
                  ],
                  "assistantMessage": "Deux modifications sont proposées.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));
        RevisionView revision = service.processText(
                visitId, userId, organizationId, "Toux sèche, auscultation normale.")
                .revisions().getFirst();

        SessionView accepted = service.decideRevision(
                visitId, userId, organizationId, revision.id(), "ACCEPT");

        assertEquals("Toux sèche", accepted.draft().get("symptoms"));
        assertEquals("Auscultation normale", accepted.draft().get("clinicalExam"));
        assertTrue(accepted.revisions().getFirst().proposals().stream()
                .allMatch(proposal -> "ACCEPTED".equals(proposal.status())));
    }

    @Test
    void shouldBlockNewInputUntilRevisionIsDecided() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Douleur abdominale",
                      "reason": "Symptôme dicté.",
                      "uncertainty": "LOW",
                      "evidence": ["Douleur abdominale"]
                    }
                  ],
                  "assistantMessage": "Une modification est proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));
        service.processText(
                visitId, userId, organizationId, "Douleur abdominale.");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processText(
                        visitId, userId, organizationId, "Depuis deux jours."));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("AI_REVISION_DECISION_REQUIRED", exception.getReason());
    }

    @Test
    void shouldResolveClarificationUsingOnlyOriginatingTurnAndAnswer() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.chat(anyList(), anyString())).thenReturn(
                chatResponse("""
                        {
                          "changes": [],
                          "assistantMessage": "Depuis combien de temps ?",
                          "needsClarification": true,
                          "clarification": {
                            "field": "symptoms",
                            "question": "Depuis combien de temps ?",
                            "options": []
                          }
                        }
                        """),
                chatResponse("""
                        {
                          "changes": [
                            {
                              "field": "symptoms",
                              "operation": "SET",
                              "value": "Douleur abdominale depuis deux jours",
                              "reason": "Durée précisée par le médecin.",
                              "uncertainty": "LOW",
                              "evidence": ["Douleur abdominale", "Depuis deux jours"]
                            }
                          ],
                          "assistantMessage": "La durée est proposée dans le brouillon.",
                          "needsClarification": false,
                          "clarification": null
                        }
                        """));

        MessageView firstResponse = service.processText(
                visitId, userId, organizationId, "Douleur abdominale.");
        UUID clarificationId = firstResponse.clarifications().getFirst().id();

        MessageView resolved = service.answerClarification(
                visitId,
                userId,
                organizationId,
                clarificationId,
                "Depuis deux jours.");

        assertEquals("RESOLVED", resolved.clarifications().getFirst().status());
        assertEquals("Depuis deux jours.", resolved.clarifications().getFirst().answer());
        assertTrue(resolved.draft().isEmpty());
        assertEquals(1, resolved.revisions().size());
        assertEquals("CLARIFICATION", resolved.conversation().get(3).source());
    }

    @Test
    void shouldRejectUnknownClarification() {
        service.startSession(visitId, userId, organizationId, Map.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.answerClarification(
                        visitId,
                        userId,
                        organizationId,
                        UUID.randomUUID(),
                        "Réponse"));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("AI_CLARIFICATION_NOT_PENDING", exception.getReason());
        verify(aiProvider, never()).chat(anyList(), anyString());
    }

    @Test
    void shouldRejectUnstructuredClarificationFromProvider() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [],
                  "assistantMessage": "Précisez la douleur.",
                  "needsClarification": true,
                  "clarification": null
                }
                """));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processText(
                        visitId, userId, organizationId, "Douleur."));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("AI_CLARIFICATION_INVALID", exception.getReason());
    }

    @Test
    void shouldTranscribeWithoutCallingChatBeforeMedicalReview() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription("Douleur à gauche", "fr", null));

        TranscriptionView response = service.transcribeAudio(
                visitId,
                userId,
                organizationId,
                new byte[] {1, 2, 3},
                "audio/webm;codecs=opus");

        assertEquals("Douleur à gauche", response.transcript());
        assertEquals("PENDING_REVIEW", response.status());
        verify(aiProvider, never()).chat(anyList(), anyString());
        SessionView session = service.getSession(
                visitId, userId, organizationId).orElseThrow();
        assertEquals("Douleur à gauche", session.pendingTranscript());
        assertTrue(session.draft().isEmpty());
    }

    @Test
    void shouldStageRealtimeTranscriptWithoutCallingChatBeforeMedicalReview() {
        service.startSession(visitId, userId, organizationId, Map.of());

        TranscriptionView response = service.stageRealtimeTranscript(
                visitId, userId, organizationId, "Patient sans fièvre");

        assertEquals("Patient sans fièvre", response.transcript());
        assertEquals("PENDING_REVIEW", response.status());
        verify(aiProvider, never()).chat(anyList(), anyString());
        SessionView session = service.getSession(
                visitId, userId, organizationId).orElseThrow();
        assertEquals("Patient sans fièvre", session.pendingTranscript());
        assertTrue(session.revisions().isEmpty());
    }

    @Test
    void shouldKeepLowConfidenceTranscriptionPendingForMedicalReview() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription(
                        "Texte clinique incertain",
                        "fr",
                        0.1));

        TranscriptionView response = service.transcribeAudio(
                visitId, userId, organizationId, new byte[] {1}, "audio/webm");

        assertEquals("Texte clinique incertain", response.transcript());
        assertEquals("PENDING_REVIEW", response.status());
        verify(aiProvider, never()).chat(anyList(), anyString());
        SessionView session = service.getSession(
                visitId, userId, organizationId).orElseThrow();
        assertEquals("Texte clinique incertain", session.pendingTranscript());
        assertTrue(session.revisions().isEmpty());
    }

    @Test
    void shouldRejectLowConfidenceRealtimeBeforeClinicalAnalysis() {
        service.startSession(visitId, userId, organizationId, Map.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.processRealtimeTranscript(
                        visitId, userId, organizationId, "Texte incertain", 0.1));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        assertEquals("AI_TRANSCRIPTION_LOW_CONFIDENCE", exception.getReason());
        verify(aiProvider, never()).chat(anyList(), anyString());
    }

    @Test
    void shouldAnalyzeCorrectedTranscriptIntoPendingRevision() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.transcribeAudio(any(byte[].class), eq("audio/webm"), eq("fr")))
                .thenReturn(new AiTranscription("Douleur à gauche", "fr", null));
        service.transcribeAudio(
                visitId, userId, organizationId, new byte[] {1}, "audio/webm");
        when(aiProvider.chat(anyList(), anyString())).thenReturn(chatResponse("""
                {
                  "changes": [
                    {
                      "field": "symptoms",
                      "operation": "SET",
                      "value": "Douleur à droite et non à gauche",
                      "reason": "Latéralité corrigée par le médecin.",
                      "uncertainty": "LOW",
                      "evidence": ["Douleur à droite et non à gauche"]
                    }
                  ],
                  "assistantMessage": "La correction est proposée.",
                  "needsClarification": false,
                  "clarification": null
                }
                """));

        MessageView response = service.analyzeTranscript(
                visitId,
                userId,
                organizationId,
                "Douleur à droite et non à gauche");

        assertEquals("Douleur à droite et non à gauche", response.transcript());
        assertTrue(response.draft().isEmpty());
        assertEquals("AUDIO", response.conversation().get(1).source());
        assertEquals("PENDING", response.revisions().getFirst().status());
        SessionView session = service.getSession(
                visitId, userId, organizationId).orElseThrow();
        assertNull(session.pendingTranscript());
        assertNull(session.transcript());
        assertEquals("ANALYZED", session.transcriptStatus());
    }

    @Test
    void shouldNotCarryRejectedOrUngroundedModelOutputIntoNextTurn() {
        service.startSession(visitId, userId, organizationId, Map.of());
        when(aiProvider.chat(anyList(), anyString())).thenReturn(
                chatResponse("""
                        {
                          "changes": [{
                            "field":"symptoms",
                            "operation":"SET",
                            "value":"invented-term",
                            "reason":"unsupported",
                            "uncertainty":"LOW",
                            "evidence":["reported cough"]
                          }],
                          "assistantMessage":"invented-term should remain",
                          "needsClarification":false,
                          "clarification":null
                        }
                        """),
                chatResponse("""
                        {
                          "changes": [{
                            "field":"symptoms",
                            "operation":"SET",
                            "value":"reported headache",
                            "reason":"explicit",
                            "uncertainty":"LOW",
                            "evidence":["reported headache"]
                          }],
                          "assistantMessage":"second turn",
                          "needsClarification":false,
                          "clarification":null
                        }
                        """));

        MessageView first = service.processText(
                visitId, userId, organizationId, "reported cough");
        assertTrue(first.revisions().isEmpty());
        service.processText(
                visitId, userId, organizationId, "reported headache");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiProvider, times(2)).chat(captor.capture(), anyString());
        List<AiMessage> secondTurnMessages = captor.getAllValues().get(1);
        assertFalse(secondTurnMessages.stream().anyMatch(message ->
                message.content().contains("invented-term")));
    }

    @Test
    void shouldRejectUnsupportedAudioBeforeCallingProvider() {
        service.startSession(visitId, userId, organizationId, Map.of());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.transcribeAudio(
                        visitId,
                        userId,
                        organizationId,
                        new byte[] {1},
                        "audio/ogg"));

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
        verify(aiProvider, never()).transcribeAudio(
                any(byte[].class), anyString(), anyString());
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

    private AiChatResponse chatResponse(String content) {
        return new AiChatResponse(content, 80, "gpt-4.1");
    }
}
