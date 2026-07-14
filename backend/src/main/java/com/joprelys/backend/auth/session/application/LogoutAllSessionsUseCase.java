package com.joprelys.backend.auth.session.application;

public interface LogoutAllSessionsUseCase {

    void logoutAll(SessionActor actor);
}
