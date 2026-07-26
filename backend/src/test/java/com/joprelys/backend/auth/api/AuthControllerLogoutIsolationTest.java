package com.joprelys.backend.auth.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.application.AuthenticationService;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.session.api.SessionActorFactory;
import com.joprelys.backend.auth.session.application.LogoutCurrentSessionUseCase;
import com.joprelys.backend.auth.session.application.RefreshAuthSessionUseCase;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthControllerLogoutIsolationTest {

    @Mock AuthenticationService authenticationService;
    @Mock RefreshAuthSessionUseCase refreshAuthSessionUseCase;
    @Mock LogoutCurrentSessionUseCase logoutCurrentSessionUseCase;
    @Mock SessionActorFactory actorFactory;
    @Mock SessionClientMetadataFactory metadataFactory;
    @Mock RefreshTokenCookieManager cookieManager;
    @Mock AuthResponseMapper responseMapper;
    @Mock Authentication authentication;
    @Mock HttpServletResponse response;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authenticationService,
                refreshAuthSessionUseCase,
                logoutCurrentSessionUseCase,
                actorFactory,
                metadataFactory,
                cookieManager,
                responseMapper);
    }

    @Test
    void shouldRevokePatientJwtWithoutClearingProfessionalRefreshCookie() {
        JwtClaims claims = claims("PATIENT", "");
        when(actorFactory.claims(authentication)).thenReturn(claims);

        controller.logout(authentication, response);

        verify(logoutCurrentSessionUseCase).logout(claims);
        verify(cookieManager, never()).clear(response);
    }

    @Test
    void shouldClearProfessionalRefreshCookieOnProfessionalLogout() {
        JwtClaims claims = claims("MEDECIN", "session-123");
        when(actorFactory.claims(authentication)).thenReturn(claims);

        controller.logout(authentication, response);

        verify(logoutCurrentSessionUseCase).logout(claims);
        verify(cookieManager).clear(response);
    }

    private static JwtClaims claims(String role, String sessionId) {
        return new JwtClaims(
                "00000000-0000-0000-0000-000000000001",
                role.toLowerCase() + "@joprelys.local",
                role,
                role,
                "00000000-0000-0000-0000-000000000002",
                "token-id",
                sessionId,
                Instant.parse("2099-01-01T00:00:00Z"));
    }
}
