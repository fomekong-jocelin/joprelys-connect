package com.joprelys.backend.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "joprelys.security.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotBlank @Size(min = 32) String secret,
		@Min(5) long ttlMinutes) {

	/**
	 * SHA-256 fingerprints of signing secrets that were published in the repository history.
	 * Anyone holding them can forge tokens, so the application refuses to start with them.
	 */
	private static final Set<String> COMPROMISED_SECRET_FINGERPRINTS = Set.of(
			"5e9b2a32334daa94d82332017c8d22b9456928df903ef69bb3695acf55518c25");

	public JwtProperties {
		if (secret != null && COMPROMISED_SECRET_FINGERPRINTS.contains(sha256Hex(secret))) {
			throw new IllegalStateException(
					"JOPRELYS_JWT_SECRET uses a compromised value published in the repository history; "
							+ "generate a new secret (openssl rand -base64 48)");
		}
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
