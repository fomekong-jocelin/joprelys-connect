package com.joprelys.backend.auth.security;

import com.joprelys.backend.auth.config.JwtProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String HMAC_SHA_256 = "HmacSHA256";
	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private final JwtProperties properties;
	private final Clock clock;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public CreatedToken createToken(UserAccountEntity user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.ttlMinutes(), ChronoUnit.MINUTES);
		String tokenId = UUID.randomUUID().toString();

		String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
		String claims = "{"
				+ "\"iss\":\"" + escapeJson(properties.issuer()) + "\","
				+ "\"aud\":\"" + escapeJson(properties.audience()) + "\","
				+ "\"sub\":\"" + escapeJson(user.getId().toString()) + "\","
				+ "\"email\":\"" + escapeJson(user.getEmail()) + "\","
				+ "\"name\":\"" + escapeJson(user.getDisplayName()) + "\","
				+ "\"role\":\"" + escapeJson(user.getRole()) + "\","
				+ "\"org\":\"" + (user.getOrganizationId() != null ? user.getOrganizationId().toString() : "") + "\","
				+ "\"jti\":\"" + escapeJson(tokenId) + "\","
				+ "\"iat\":" + issuedAt.getEpochSecond() + ","
				+ "\"exp\":" + expiresAt.getEpochSecond()
				+ "}";

		String unsignedToken = encodeBase64Url(header) + "." + encodeBase64Url(claims);
		String signature = sign(unsignedToken);
		return new CreatedToken(unsignedToken + "." + signature, tokenId, expiresAt);
	}

	public CreatedToken createPatientToken(com.joprelys.backend.patient.infrastructure.persistence.PatientEntity patient) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.ttlMinutes(), ChronoUnit.MINUTES);
		String tokenId = UUID.randomUUID().toString();

		String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
		String claims = "{"
				+ "\"iss\":\"" + escapeJson(properties.issuer()) + "\","
				+ "\"aud\":\"" + escapeJson(properties.audience()) + "\","
				+ "\"sub\":\"" + escapeJson(patient.getGlobalPatientNumber()) + "\","
				+ "\"email\":\"" + escapeJson(patient.getGlobalPatientNumber()) + "\","
				+ "\"name\":\"" + escapeJson(patient.getFullName()) + "\","
				+ "\"role\":\"PATIENT\","
				+ "\"org\":\"" + (patient.getOrganizationId() != null ? patient.getOrganizationId().toString() : "") + "\","
				+ "\"jti\":\"" + escapeJson(tokenId) + "\","
				+ "\"iat\":" + issuedAt.getEpochSecond() + ","
				+ "\"exp\":" + expiresAt.getEpochSecond()
				+ "}";

		String unsignedToken = encodeBase64Url(header) + "." + encodeBase64Url(claims);
		String signature = sign(unsignedToken);
		return new CreatedToken(unsignedToken + "." + signature, tokenId, expiresAt);
	}

	public JwtClaims parseAndValidate(String token) {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new InvalidTokenException("Invalid token format");
		}

		validateSignature(parts[0] + "." + parts[1], parts[2]);
		String header = decodeBase64Url(parts[0]);
		if (!"HS256".equals(requiredString(header, "alg"))) {
			throw new InvalidTokenException("Unsupported token algorithm");
		}

		String claims = decodeBase64Url(parts[1]);
		validateRegisteredClaims(claims);

		return new JwtClaims(
				requiredString(claims, "sub"),
				requiredString(claims, "email"),
				requiredString(claims, "name"),
				requiredString(claims, "role"),
				optionalString(claims, "org"),
				requiredString(claims, "jti"),
				Instant.ofEpochSecond(requiredNumber(claims, "exp")));
	}

	private void validateRegisteredClaims(String claims) {
		if (!properties.issuer().equals(requiredString(claims, "iss"))) {
			throw new InvalidTokenException("Invalid issuer");
		}
		if (!properties.audience().equals(requiredString(claims, "aud"))) {
			throw new InvalidTokenException("Invalid audience");
		}
		if (!Instant.ofEpochSecond(requiredNumber(claims, "exp")).isAfter(clock.instant())) {
			throw new InvalidTokenException("Expired token");
		}
	}

	private void validateSignature(String unsignedToken, String actualSignature) {
		byte[] expected = sign(unsignedToken).getBytes(StandardCharsets.UTF_8);
		byte[] actual = actualSignature.getBytes(StandardCharsets.UTF_8);
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new InvalidTokenException("Invalid token signature");
		}
	}

	private static String encodeBase64Url(String value) {
		return BASE64_URL_ENCODER.encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String decodeBase64Url(String value) {
		try {
			return new String(BASE64_URL_DECODER.decode(value), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new InvalidTokenException("Unable to decode token", exception);
		}
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA_256);
			mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to sign JWT", exception);
		}
	}

	private static String requiredString(String json, String claimName) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(claimName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
				.matcher(json);
		if (!matcher.find() || matcher.group(1).isBlank()) {
			throw new InvalidTokenException("Missing token claim: " + claimName);
		}
		return unescapeJson(matcher.group(1));
	}

	private static String optionalString(String json, String claimName) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(claimName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
				.matcher(json);
		if (!matcher.find()) {
			return "";
		}
		return unescapeJson(matcher.group(1));
	}

	private static long requiredNumber(String json, String claimName) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(claimName) + "\"\\s*:\\s*(\\d+)")
				.matcher(json);
		if (!matcher.find()) {
			throw new InvalidTokenException("Missing numeric token claim: " + claimName);
		}
		return Long.parseLong(matcher.group(1));
	}

	private static String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String unescapeJson(String value) {
		return value.replace("\\\"", "\"").replace("\\\\", "\\");
	}

	public record CreatedToken(String value, String tokenId, Instant expiresAt) {
	}
}
