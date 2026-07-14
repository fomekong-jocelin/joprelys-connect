package com.joprelys.backend.auth.session.application;

public interface RefreshAuthSessionUseCase {
    IssuedAuthSession refresh(String refreshToken, SessionClientMetadata metadata);
}
