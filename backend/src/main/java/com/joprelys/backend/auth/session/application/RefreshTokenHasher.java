package com.joprelys.backend.auth.session.application;

public interface RefreshTokenHasher {
    String hash(String token);
}
