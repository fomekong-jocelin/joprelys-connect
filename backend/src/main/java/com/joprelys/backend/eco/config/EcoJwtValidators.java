package com.joprelys.backend.eco.config;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Règles de validation des jetons Joprelys ID : signature (décodeur), expiration, émetteur et audience. */
public final class EcoJwtValidators {

    private EcoJwtValidators() {
    }

    public static OAuth2TokenValidator<Jwt> create(String issuerUri, String audience) {
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience non autorisée", null));
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuerUri), audienceValidator);
    }
}
