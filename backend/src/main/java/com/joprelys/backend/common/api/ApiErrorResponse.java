package com.joprelys.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO représentant le format d'erreur normalisé du CDC Module 16.
 * {
 *   "error": {
 *     "code": "ACCESS_DENIED",
 *     "message": "...",
 *     "trace_id": "trc_...",
 *     "action": "REQUEST_ACCESS",
 *     "required_scope": "medical_records"
 *   }
 * }
 */
public record ApiErrorResponse(ErrorDetails error) {

    public ApiErrorResponse(String code, String message, String traceId) {
        this(new ErrorDetails(code, message, traceId, null, null));
    }

    public ApiErrorResponse(
            String code,
            String message,
            String traceId,
            String action,
            String requiredScope) {
        this(new ErrorDetails(code, message, traceId, action, requiredScope));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetails(
            String code,
            String message,
            String trace_id,
            String action,
            String required_scope) {}
}
