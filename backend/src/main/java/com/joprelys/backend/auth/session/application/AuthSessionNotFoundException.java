package com.joprelys.backend.auth.session.application;

public class AuthSessionNotFoundException extends RuntimeException {

    public AuthSessionNotFoundException() {
        super("AUTH_SESSION_NOT_FOUND");
    }
}
