package com.joprelys.backend.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joprelys.security.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotBlank @Size(min = 32) String secret,
		@Min(5) long ttlMinutes) {
}
