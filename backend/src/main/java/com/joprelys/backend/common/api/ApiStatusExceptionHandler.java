package com.joprelys.backend.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles explicit business/API status errors before the generic status handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiStatusExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiStatusExceptionHandler.class);

    @ExceptionHandler(ApiStatusException.class)
    ResponseEntity<ApiErrorResponse> handleApiStatus(ApiStatusException ex) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId == null) {
            traceId = "trc_unknown";
        }

        String message = ex.getReason() != null
                ? ex.getReason()
                : ex.getStatusCode().toString();
        log.warn(
                "[trace_id={}] ApiStatusException: {} {} - {}",
                traceId,
                ex.getStatusCode().value(),
                ex.apiCode(),
                message);

        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        ex.apiCode(),
                        message,
                        traceId,
                        ex.action(),
                        ex.requiredScope()));
    }
}
