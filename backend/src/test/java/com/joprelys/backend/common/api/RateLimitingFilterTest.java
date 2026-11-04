package com.joprelys.backend.common.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingProperties properties;
    private MutableClock clock;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setMaxRequestsPerWindow(3);
        properties.setWindowSeconds(60);
        clock = new MutableClock(Instant.parse("2026-11-04T08:00:00Z"));
        filter = new RateLimitingFilter(properties, clock);
    }

    @AfterEach
    void tearDown() {
        org.slf4j.MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
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
        filter = new RateLimitingFilter(properties, clock);

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

    @Test
    void givenSpoofedForwardedForHeaders_whenFilter_thenClientStaysInItsOwnBucket() throws ServletException, IOException {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("203.0.113.7");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletRequest spoofed = new MockHttpServletRequest();
        spoofed.setRemoteAddr("203.0.113.7");
        spoofed.addHeader("X-Forwarded-For", "198.51.100.250");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(spoofed, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
    }

    @Test
    void givenIdleClients_whenWindowElapses_thenTheirBucketsArePurged() throws ServletException, IOException {
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.1.0." + i);
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }
        assertEquals(20, filter.trackedClients());

        clock.advance(Duration.ofSeconds(121));
        MockHttpServletRequest late = new MockHttpServletRequest();
        late.setRemoteAddr("10.2.0.1");
        filter.doFilterInternal(late, new MockHttpServletResponse(), new MockFilterChain());

        assertEquals(1, filter.trackedClients());
    }

    @Test
    void givenMaliciousTraceIdInContext_whenRateLimited_thenItIsNotReflectedInTheJsonBody()
            throws ServletException, IOException {
        org.slf4j.MDC.put(TraceIdFilter.TRACE_ID_MDC_KEY, "x\",\"injected\":\"1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.9.9");
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(429, response.getStatus());
        String body = response.getContentAsString();
        assertFalse(body.contains("injected"));
        assertTrue(body.contains("\"trace_id\":\"trc_unknown\""));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
