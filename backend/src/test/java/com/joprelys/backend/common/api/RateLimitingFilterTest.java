package com.joprelys.backend.common.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingProperties properties;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setMaxRequestsPerWindow(3);
        properties.setWindowSeconds(60);
        filter = new RateLimitingFilter(properties);
    }

    @Test
    void givenUnderLimit_whenFilter_thenRequestAllowed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void givenOverLimit_whenFilter_thenRequestBlocked() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.2");

        // 3 requêtes OK
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        // 4ème requête = bloquée
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body.contains("RATE_LIMITED"));
    }

    @Test
    void givenDisabled_whenFilter_thenRequestAlwaysAllowed() throws ServletException, IOException {
        properties.setEnabled(false);
        filter = new RateLimitingFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.3");

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void givenAuthenticatedUser_whenFilter_thenLimitPerUser() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteUser("doctor@clinic.com");
        request.setRemoteAddr("192.168.1.4");

        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        assertEquals(429, response.getStatus());
    }

    @Test
    void givenDifferentIps_whenFilter_thenLimitsAreIndependent() throws ServletException, IOException {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0." + i);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }
}
