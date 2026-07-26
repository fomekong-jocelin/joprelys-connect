package com.joprelys.backend.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.application.AiConsultationContract.SessionView;
import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class AiConsultationControllerTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void shouldUseNormalizedAuthenticatedIdentity() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(authentication.getDetails()).thenReturn(claims(userId.toString(), organizationId.toString()));
        TenantContext.setTenantId(organizationId);
        when(service.startSession(visitId, userId, organizationId, Map.of()))
                .thenReturn(new SessionView(
                        UUID.randomUUID(),
                        visitId,
                        "ACTIVE",
                        Instant.now().plusSeconds(1800),
                        Map.of(),
                        null,
                        null,
                        "NONE",
                        List.of(),
                        List.of(),
                        List.of(),
                        "Décrivez les symptômes.",
                        false));

        SessionView response = controller.startSession(
                visitId,
                null,
                authentication);

        assertNotNull(response);
        assertEquals(visitId, response.visitId());
        verify(service).startSession(visitId, userId, organizationId, Map.of());
    }

    @Test
    void shouldRouteClarificationAnswerWithAuthenticatedIdentity() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        UUID clarificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(authentication.getDetails()).thenReturn(claims(userId.toString(), organizationId.toString()));
        TenantContext.setTenantId(organizationId);

        controller.answerClarification(
                visitId,
                clarificationId,
                new AiConsultationController.ClarificationAnswerRequest("Depuis deux jours"),
                authentication);

        verify(service).answerClarification(
                visitId,
                userId,
                organizationId,
                clarificationId,
                "Depuis deux jours");
    }

    @Test
    void shouldRouteProposalDecisionWithAuthenticatedIdentity() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID proposalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(authentication.getDetails()).thenReturn(claims(userId.toString(), organizationId.toString()));
        TenantContext.setTenantId(organizationId);

        controller.decideProposal(
                visitId,
                revisionId,
                proposalId,
                new AiConsultationController.DecisionRequest("ACCEPT"),
                authentication);

        verify(service).decideProposal(
                visitId,
                userId,
                organizationId,
                revisionId,
                proposalId,
                "ACCEPT");
    }

    @Test
    void shouldStageRealtimeTranscriptWithAuthenticatedIdentity() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        when(authentication.getDetails()).thenReturn(claims(userId.toString(), organizationId.toString()));
        TenantContext.setTenantId(organizationId);

        controller.stageRealtimeTranscript(
                visitId,
                new AiConsultationController.AnalyzeTranscriptRequest("Patient sans fièvre"),
                authentication);

        verify(service).stageRealtimeTranscript(
                visitId,
                userId,
                organizationId,
                "Patient sans fièvre");
    }

    @Test
    void shouldRejectUnresolvedLegacyIdentityAtControllerBoundary() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getDetails()).thenReturn(
                claims("doctor@joprelys.local", UUID.randomUUID().toString()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSession(UUID.randomUUID(), authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("AUTH_IDENTITY_UNRESOLVED", exception.getReason());
    }

    @Test
    void shouldRejectMissingTenantAtControllerBoundary() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getDetails()).thenReturn(claims(UUID.randomUUID().toString(), ""));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.getSession(UUID.randomUUID(), authentication));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("AUTH_TENANT_UNRESOLVED", exception.getReason());
    }

    private static JwtClaims claims(String subject, String organizationId) {
        return new JwtClaims(
                subject,
                "doctor@joprelys.local",
                "Doctor",
                "MEDECIN",
                organizationId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(3600));
    }
}
