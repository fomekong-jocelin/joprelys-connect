package com.joprelys.backend.common.api;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles explicit business/API status errors before the generic handler.
 *
 * <p>Legacy consent/scope reasons are normalized as a safety net so historical
 * services such as FHIR or hospitalization cannot expose an opaque ACCESS_DENIED.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiStatusExceptionHandler {

    @ExceptionHandler(ApiStatusException.class)
    ResponseEntity<ApiErrorResponse> handleApiStatus(ApiStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        ex.apiCode(),
                        reason(ex),
                        traceId(),
                        ex.action(),
                        ex.requiredScope()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleLegacyResponseStatus(ResponseStatusException ex) {
        String traceId = traceId();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String rawReason = reason(ex);

        if (status == HttpStatus.FORBIDDEN && "CONSENT_REQUIRED".equals(rawReason)) {
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    "CONSENT_REQUIRED",
                    "Le patient n'a pas autorisé l'accès à cette partie de son dossier.",
                    traceId,
                    "REQUEST_ACCESS",
                    null));
        }

        if (status == HttpStatus.FORBIDDEN && rawReason.startsWith("Scope manquant:")) {
            String scope = rawReason.substring("Scope manquant:".length()).trim();
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    "SCOPE_REQUIRED",
                    "L'accès existe, mais le périmètre demandé n'a pas été partagé.",
                    traceId,
                    "REQUEST_ACCESS",
                    scope.isBlank() ? null : scope));
        }

        String code = switch (status) {
            case BAD_REQUEST -> "BAD_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "ACCESS_DENIED";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case UNPROCESSABLE_ENTITY -> "UNPROCESSABLE_ENTITY";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            case INTERNAL_SERVER_ERROR -> "INTERNAL_ERROR";
            default -> "ERROR";
        };
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, rawReason, traceId));
    }

    private String reason(ResponseStatusException ex) {
        return ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
    }

    private String traceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : "trc_unknown";
    }
}
