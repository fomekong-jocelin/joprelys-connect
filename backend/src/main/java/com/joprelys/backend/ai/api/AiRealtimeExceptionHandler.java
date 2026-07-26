package com.joprelys.backend.ai.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
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

    private static final Logger log = LoggerFactory.getLogger(AiRealtimeExceptionHandler.class);
    private static final MediaType APPLICATION_SDP = MediaType.valueOf("application/sdp");
    private static final String ERROR_HEADER = "X-Joprelys-AI-Realtime-Error";

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<String> handleRealtimeFailure(ResponseStatusException exception) {
        String reason = normalizeReason(exception.getReason());
        return errorResponse(exception.getStatusCode().value(), reason);
    }

    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<String> handleUnexpectedRealtimeFailure(RuntimeException exception) {
        log.error("Échec Realtime inattendu type={}", exception.getClass().getSimpleName());
        return errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI_REALTIME_UNAVAILABLE");
    }

    private ResponseEntity<String> errorResponse(int status, String reason) {
        return ResponseEntity.status(status)
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
