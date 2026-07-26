package com.joprelys.backend.ai.api;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
        AiRealtimeController.class,
        AiRealtimeVitalsController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiRealtimeExceptionHandler {

    private static final MediaType APPLICATION_SDP = MediaType.valueOf("application/sdp");
    private static final String ERROR_HEADER = "X-Joprelys-AI-Realtime-Error";

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<String> handleRealtimeFailure(ResponseStatusException exception) {
        String reason = normalizeReason(exception.getReason());
        return ResponseEntity.status(exception.getStatusCode())
                .cacheControl(CacheControl.noStore())
                .header(ERROR_HEADER, reason)
                .contentType(APPLICATION_SDP)
                .body(reason);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank() || !reason.startsWith("AI_REALTIME_")) {
            return "AI_REALTIME_UNAVAILABLE";
        }
        return reason.trim();
    }
}
