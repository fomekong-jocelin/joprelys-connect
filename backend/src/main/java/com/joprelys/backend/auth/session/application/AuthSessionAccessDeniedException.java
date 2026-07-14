package com.joprelys.backend.auth.session.application;

public class AuthSessionAccessDeniedException extends RuntimeException {

    public AuthSessionAccessDeniedException() {
        super("ACCESS_DENIED");
    }
}
