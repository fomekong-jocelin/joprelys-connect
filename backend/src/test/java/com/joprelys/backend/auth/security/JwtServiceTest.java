package com.joprelys.backend.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.joprelys.backend.auth.config.JwtProperties;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T10:00:00Z"), ZoneOffset.UTC);

	private final JwtService jwtService = new JwtService(
			new JwtProperties(
					"joprelys-connect-test",
					"joprelys-clinic-test",
					"test-jwt-secret-for-joprelys-connect-32",
					30),
			FIXED_CLOCK);

	@Test
	void shouldCreateAndValidateToken() {
		UserAccountEntity user = new UserAccountEntity(
				"agent@example.com",
				"Agent Accueil",
				"AGENT_ACCUEIL",
				"hash");

		JwtService.CreatedToken token = jwtService.createToken(user);
		JwtClaims claims = jwtService.parseAndValidate(token.value());

		assertEquals("agent@example.com", claims.email());
		assertEquals("Agent Accueil", claims.displayName());
		assertEquals("AGENT_ACCUEIL", claims.role());
		assertEquals("", claims.organizationId());
		assertEquals(token.tokenId(), claims.tokenId());
		assertEquals(Instant.parse("2026-07-02T10:30:00Z"), claims.expiresAt());
	}

	@Test
	void shouldCreateAndValidateTokenWithOrganization() {
		UserAccountEntity user = new UserAccountEntity(
				"agent@example.com",
				"Agent Accueil",
				"AGENT_ACCUEIL",
				"hash");
		java.util.UUID orgId = java.util.UUID.randomUUID();
		user.setOrganizationId(orgId);

		JwtService.CreatedToken token = jwtService.createToken(user);
		JwtClaims claims = jwtService.parseAndValidate(token.value());

		assertEquals("agent@example.com", claims.email());
		assertEquals(orgId.toString(), claims.organizationId());
	}

	@Test
	void shouldRejectTamperedToken() {
		UserAccountEntity user = new UserAccountEntity(
				"agent@example.com",
				"Agent Accueil",
				"AGENT_ACCUEIL",
				"hash");

		String token = jwtService.createToken(user).value();
		char replacement = token.endsWith("x") ? 'y' : 'x';
		String tamperedToken = token.substring(0, token.length() - 1) + replacement;

		assertThrows(InvalidTokenException.class, () -> jwtService.parseAndValidate(tamperedToken));
	}
}
