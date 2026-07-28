package com.joprelys.backend.common.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles business/API status errors before the generic status handler.
 *
 * <p>Legacy consent errors are normalized here as a safety net so an older
 * controller cannot silently fall back to a generic ACCESS_DENIED response.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiStatusExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiStatusExceptionHandler.class);

    @ExceptionHandler(ApiStatusException.class)
    ResponseEntity<ApiErrorResponse> handleApiStatus(ApiStatusException ex) {
        String traceId = traceId();
        String message = reason(ex);
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

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleLegacyResponseStatus(ResponseStatusException ex) {
        String traceId = traceId();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String rawReason = reason(ex);

        if (status == HttpStatus.FORBIDDEN && "CONSENT_REQUIRED".equals(rawReason)) {
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    "CONSENT_REQUIRED",
                    "Le patient n'a pas autorisé l'accès à cette partie de son dossier. Demandez son accès.",
                    traceId,
                    "REQUEST_ACCESS",
                    null));
        }

        if (status == HttpStatus.FORBIDDEN && rawReason.startsWith("Scope manquant:")) {
            String scope = rawReason.substring("Scope manquant:".length()).trim();
            return ResponseEntity.status(status).body(new ApiErrorResponse(
                    "SCOPE_REQUIRED",
                    "L'accès existe, mais le périmètre demandé n'a pas été partagé. Demandez une extension d'accès.",
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
