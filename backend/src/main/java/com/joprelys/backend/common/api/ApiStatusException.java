package com.joprelys.backend.common.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * ResponseStatusException carrying a stable machine-readable API code and
 * optional remediation metadata for the frontend.
 */
public class ApiStatusException extends ResponseStatusException {

    private final String apiCode;
    private final String action;
    private final String requiredScope;

    public ApiStatusException(HttpStatus status, String apiCode, String message) {
        this(status, apiCode, message, null, null);
    }

    public ApiStatusException(
            HttpStatus status,
            String apiCode,
            String message,
            String action,
            String requiredScope) {
        super(status, message);
        this.apiCode = apiCode;
        this.action = action;
        this.requiredScope = requiredScope;
    }

    public String apiCode() {
        return apiCode;
    }

    public String action() {
        return action;
    }

    public String requiredScope() {
        return requiredScope;
    }
}
