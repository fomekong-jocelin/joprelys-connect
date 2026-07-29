package com.joprelys.backend.common.api;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles explicit business access errors before the generic API handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiStatusExceptionHandler {

    @ExceptionHandler(ApiStatusException.class)
    ResponseEntity<ApiErrorResponse> handleApiStatus(ApiStatusException ex) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId == null) traceId = "trc_unknown";
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        ex.apiCode(),
                        message,
                        traceId,
                        ex.action(),
                        ex.requiredScope()));
    }
}
