package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.security.JwtClaims;

public interface AccessTokenSessionValidator {

    boolean isValid(JwtClaims claims);
}
