package com.joprelys.backend.ai.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AiRealtimeExceptionHandlerTest {

    private final AiRealtimeExceptionHandler handler = new AiRealtimeExceptionHandler();

    @Test
    void shouldExposeRealtimeReasonInBodyAndHeader() {
        var response = handler.handleRealtimeFailure(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_REALTIME_QUOTA_EXCEEDED"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("AI_REALTIME_QUOTA_EXCEEDED", response.getBody());
        assertEquals(
                "AI_REALTIME_QUOTA_EXCEEDED",
                response.getHeaders().getFirst("X-Joprelys-AI-Realtime-Error"));
        assertEquals("application/sdp", response.getHeaders().getContentType().toString());
    }

    @Test
    void shouldNotLeakNonRealtimeExceptionMessages() {
        var response = handler.handleRealtimeFailure(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "sensitive upstream detail"));

        assertEquals("AI_REALTIME_UNAVAILABLE", response.getBody());
    }
}
