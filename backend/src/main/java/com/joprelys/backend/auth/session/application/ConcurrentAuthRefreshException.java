package com.joprelys.backend.auth.session.application;

/**
 * Signals a near-simultaneous refresh from the same browser/client context.
 * This is intentionally distinct from a refresh-token replay attack so that
 * the active token family is not revoked because of a benign browser race.
 */
public class ConcurrentAuthRefreshException extends RuntimeException {

    public ConcurrentAuthRefreshException() {
        super("AUTH_REFRESH_CONCURRENT");
    }
}
