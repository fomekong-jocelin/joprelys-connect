package com.joprelys.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class BearerTokenResolverTest {

    private final BearerTokenResolver resolver = new BearerTokenResolver();

    @Test
    void resolvesAuthorizationHeaderFirst() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/voice/stream");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token");
        request.setParameter("token", "query-token");

        assertThat(resolver.resolve(request)).contains("header-token");
    }

    @Test
    void resolvesQueryTokenForVoiceWebSocketHandshake() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/voice/stream");
        request.setParameter("token", "voice-query-token");

        assertThat(resolver.resolve(request)).contains("voice-query-token");
    }

    @Test
    void ignoresQueryTokenOutsideVoiceWebSocketHandshake() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/patients");
        request.setParameter("token", "must-not-authenticate-rest-request");

        assertThat(resolver.resolve(request)).isEmpty();
    }

    @Test
    void ignoresBlankVoiceQueryToken() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/voice/stream");
        request.setParameter("token", "   ");

        assertThat(resolver.resolve(request)).isEmpty();
    }
}
