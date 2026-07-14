package com.joprelys.backend.auth.session.application;

public class InvalidAuthSessionException extends RuntimeException {

    public InvalidAuthSessionException() {
        super("AUTH_SESSION_INVALID");
    }
}
