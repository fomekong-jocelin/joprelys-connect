package com.joprelys.backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Normalized API error envelope.
 *
 * <p>The nested {@code error} object is the canonical contract. The top-level
 * {@code detail} property is intentionally retained during migration because older
 * Joprelys screens and integrations still read it. Both values always carry the
 * same human-readable message.</p>
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

    @JsonProperty("detail")
    public String detail() {
        return error == null ? null : error.message();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorDetails(
            String code,
            String message,
            String trace_id,
            String action,
            String required_scope) {}
}
