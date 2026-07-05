package com.joprelys.backend.common.api;

/**
 * DTO représentant le format d'erreur normalisé du CDC Module 16.
 * {
 *   "error": {
 *     "code": "ACCESS_DENIED",
 *     "message": "...",
 *     "trace_id": "trc_..."
 *   }
 * }
 */
public record ApiErrorResponse(ErrorDetails error) {

    public ApiErrorResponse(String code, String message, String traceId) {
        this(new ErrorDetails(code, message, traceId));
    }

    public record ErrorDetails(String code, String message, String trace_id) {}
}
