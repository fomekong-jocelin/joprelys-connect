package com.joprelys.backend.ai.infrastructure.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.joprelys.backend.auth.security.JwtClaims;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;

class WebSocketAuthInterceptorTest {

    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedHandshakeIsRejectedWith401() {
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        boolean accepted = handshake(servletResponse, new HashMap<>());

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(401);
    }

    @Test
    void authenticatedUserWithoutClinicalPermissionIsRejectedWith403() {
        authenticate("PATIENT_READ");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = handshake(servletResponse, attributes);

        assertThat(accepted).isFalse();
        assertThat(servletResponse.getStatus()).isEqualTo(403);
        assertThat(attributes).isEmpty();
    }

    @Test
    void clinicianWithClinicalWriteIsAcceptedWithResolvedIdentity() {
        authenticate("CLINICAL_WRITE");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = handshake(new MockHttpServletResponse(), attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes)
                .containsEntry("userId", userId)
                .containsEntry("organizationId", organizationId)
                .containsEntry("userEmail", "medecin@joprelys.local");
    }

    @Test
    void wildcardOrEmptyAllowedOriginsAreRefused() {
        assertThatThrownBy(() -> WebSocketConfig.parseAllowedOrigins("*")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> WebSocketConfig.parseAllowedOrigins("https://app.joprelys.com, https://*.evil.test"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> WebSocketConfig.parseAllowedOrigins(" , ")).isInstanceOf(IllegalStateException.class);
        assertThat(WebSocketConfig.parseAllowedOrigins("https://joprelys.com, https://app.joprelys.com"))
                .containsExactly("https://joprelys.com", "https://app.joprelys.com");
    }

    private boolean handshake(MockHttpServletResponse servletResponse, Map<String, Object> attributes) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/api/voice/stream");
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                attributes);
    }

    private void authenticate(String permission) {
        JwtClaims claims = new JwtClaims(userId.toString(), "medecin@joprelys.local", "Dr. Test", "MEDECIN",
                organizationId.toString(), "jti", "sid", Instant.now().plusSeconds(600));
        var authentication = new UsernamePasswordAuthenticationToken(
                claims.email(), "token", List.of(new SimpleGrantedAuthority(permission)));
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
