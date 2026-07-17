package com.joprelys.backend.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.ai.application.AiConsultationService;
import com.joprelys.backend.auth.security.JwtClaims;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

class AiConsultationControllerTest {

    @Test
    void shouldAcceptLegacyEmailSubjectWithoutReturningServerError() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims(
                "doctor@joprelys.local",
                "doctor@joprelys.local",
                "Doctor",
                "MEDECIN",
                organizationId.toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(3600));
        when(authentication.getDetails()).thenReturn(claims);
        when(service.startSession(eq(visitId), any(UUID.class), eq(organizationId), eq(Map.of())))
                .thenAnswer(invocation -> new AiConsultationService.SessionView(
                        UUID.randomUUID(),
                        visitId,
                        "ACTIVE",
                        Instant.now().plusSeconds(1800),
                        Map.of(),
                        null,
                        "Décrivez les symptômes.",
                        false));

        AiConsultationService.SessionView response = controller.startSession(
                visitId,
                null,
                authentication);

        assertNotNull(response);
        assertEquals(visitId, response.visitId());
        ArgumentCaptor<UUID> actorId = ArgumentCaptor.forClass(UUID.class);
        verify(service).startSession(eq(visitId), actorId.capture(), eq(organizationId), eq(Map.of()));
        assertNotNull(actorId.getValue());
    }

    @Test
    void shouldUseSameSyntheticActorForSameLegacyEmail() {
        AiConsultationService service = mock(AiConsultationService.class);
        AiConsultationController controller = new AiConsultationController(service);
        Authentication authentication = mock(Authentication.class);
        UUID visitId = UUID.randomUUID();
        JwtClaims claims = new JwtClaims(
                "legacy-subject",
                "Doctor@Joprelys.Local",
                "Doctor",
                "MEDECIN",
                "",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                Instant.now().plusSeconds(3600));
        when(authentication.getDetails()).thenReturn(claims);
        when(service.getSession(eq(visitId), any(UUID.class), eq(null)))
                .thenReturn(java.util.Optional.empty());

        controller.getSession(visitId, authentication);
        controller.getSession(visitId, authentication);

        ArgumentCaptor<UUID> actorIds = ArgumentCaptor.forClass(UUID.class);
        verify(service, org.mockito.Mockito.times(2))
                .getSession(eq(visitId), actorIds.capture(), eq(null));
        assertEquals(actorIds.getAllValues().get(0), actorIds.getAllValues().get(1));
    }
}
