package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.security.JwtClaims;

public interface LogoutCurrentSessionUseCase {

    void logout(JwtClaims claims);
}
