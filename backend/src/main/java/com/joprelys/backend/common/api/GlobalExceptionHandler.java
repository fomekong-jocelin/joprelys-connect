package com.joprelys.backend.common.api;

import com.joprelys.backend.notification.application.MailDeliveryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;

/**
 * Gestionnaire global des exceptions API.
 * Retourne le format d'erreur normalisé du CDC Module 16 :
 * { "error": { "code", "message", "trace_id" } }
 *
 * Ce handler est complémentaire à {@link com.joprelys.backend.auth.api.AuthExceptionHandler}
 * qui gère les cas d'authentification spécifiques.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String traceId = getTraceId();
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String code = mapStatusToCode(status);
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();

        log.warn("[trace_id={}] ResponseStatusException: {} - {}", traceId, status.value(), message);

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(code, message, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = getTraceId();
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .filter(m -> m != null)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("[trace_id={}] Validation error: {}", traceId, detail);

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", detail, traceId));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        String traceId = getTraceId();
        log.warn("[trace_id={}] Access denied: {}", traceId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("ACCESS_DENIED", "Vous n'êtes pas autorisé à effectuer cette action.", traceId));
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(NoSuchElementException ex) {
        String traceId = getTraceId();
        log.warn("[trace_id={}] Not found: {}", traceId, ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", ex.getMessage(), traceId));
    }

    @ExceptionHandler(MailDeliveryUnavailableException.class)
    ResponseEntity<ApiErrorResponse> handleMailDeliveryUnavailable(MailDeliveryUnavailableException ex) {
        String traceId = getTraceId();
        log.error("[trace_id={}] Account credential delivery unavailable", traceId, ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse(
                        "MAIL_DELIVERY_UNAVAILABLE",
                        "Le service e-mail est temporairement indisponible. Veuillez réessayer dans quelques instants.",
                        traceId));
    }

    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            org.springframework.dao.OptimisticLockingFailureException.class,
            org.hibernate.StaleObjectStateException.class,
            org.hibernate.StaleStateException.class,
            org.springframework.transaction.TransactionSystemException.class
    })
    ResponseEntity<ApiErrorResponse> handleOptimisticLocking(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }

        if (cause instanceof org.hibernate.StaleObjectStateException ||
            cause instanceof org.hibernate.StaleStateException ||
            cause instanceof org.springframework.orm.ObjectOptimisticLockingFailureException ||
            cause instanceof org.springframework.dao.OptimisticLockingFailureException) {

            String traceId = getTraceId();
            log.warn("[trace_id={}] Optimistic locking failure: {}", traceId, cause.getMessage());

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse("CONFLICT", "Cette ressource a été modifiée par un autre utilisateur. Veuillez rafraîchir et réessayer.", traceId));
        }

        if (ex instanceof org.springframework.transaction.TransactionSystemException) {
            String traceId = getTraceId();
            log.error("[trace_id={}] Transaction system failure: {}", traceId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("INTERNAL_ERROR", "Une erreur interne de transaction est survenue.", traceId));
        }

        String traceId = getTraceId();
        log.warn("[trace_id={}] Locking failure fallback: {}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("CONFLICT", "Cette ressource a été modifiée par un autre utilisateur.", traceId));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        String traceId = getTraceId();
        log.warn("[trace_id={}] Resource not found: {}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse("NOT_FOUND", "La ressource demandée n'existe pas.", traceId));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        String traceId = getTraceId();
        log.warn("[trace_id={}] Method not allowed: {}", traceId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponse("METHOD_NOT_ALLOWED", "Méthode HTTP non supportée pour cette ressource.", traceId));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        String traceId = getTraceId();
        log.error("[trace_id={}] Unexpected error: {}", traceId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("INTERNAL_ERROR", "Une erreur interne est survenue. Veuillez réessayer plus tard.", traceId));
    }

    private String getTraceId() {
        String traceId = org.slf4j.MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return traceId != null ? traceId : "trc_unknown";
    }

    private String mapStatusToCode(HttpStatus status) {
        return switch (status) {
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
    }
}
