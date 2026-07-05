package com.joprelys.backend.auth.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-02T10:00:00Z"), ZoneOffset.UTC);

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private AuthAuditEventRepository authAuditEventRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtService jwtService;

	@Mock
	private JwtRevocationService jwtRevocationService;

	@Mock
	private OrganizationRepository organizationRepository;

	@Test
	void shouldReturnTokenAndAuditSuccessWhenCredentialsAreValid() {
		UserAccountEntity user = new UserAccountEntity(
				"agent@example.com",
				"Agent Accueil",
				"AGENT_ACCUEIL",
				"hash");
		AuthenticationService service = service();

		when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
		when(jwtService.createToken(user)).thenReturn(new JwtService.CreatedToken(
				"jwt-token",
				"token-id",
				Instant.parse("2026-07-02T10:30:00Z")));

		LoginResponse response = service.login(new LoginRequest("Agent@Example.com", "Password123!"), "127.0.0.1");

		assertEquals("jwt-token", response.accessToken());
		assertEquals("agent@example.com", response.email());
		assertEquals("Agent Accueil", response.name());
		assertEquals("AGENT_ACCUEIL", response.role());
		verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
	}

	@Test
	void shouldAuditAndReturnGenericErrorWhenCredentialsAreInvalid() {
		AuthenticationService service = service();
		when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.empty());

		BadCredentialsException exception = assertThrows(
				BadCredentialsException.class,
				() -> service.login(new LoginRequest("agent@example.com", "wrong"), "127.0.0.1"));

		assertEquals("Invalid email or password", exception.getMessage());
		verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
	}

	@Test
	void shouldThrowExceptionWhenOrganizationIsInactive() {
		UserAccountEntity user = new UserAccountEntity(
				"agent@example.com",
				"Agent Accueil",
				"AGENT_ACCUEIL",
				"hash");
		UUID orgId = UUID.randomUUID();
		user.setOrganizationId(orgId);

		OrganizationEntity org = new OrganizationEntity("Espoir", "espoir@joprelys.local", "123", "street", "Douala");
		org.setStatus("INACTIVE");

		AuthenticationService service = service();

		when(userAccountRepository.findByEmail("agent@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
		when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

		BadCredentialsException exception = assertThrows(
				BadCredentialsException.class,
				() -> service.login(new LoginRequest("agent@example.com", "Password123!"), "127.0.0.1"));

		assertEquals("Votre établissement est désactivé.", exception.getMessage());
		verify(authAuditEventRepository).save(any(AuthAuditEventEntity.class));
	}

	@Test
	void shouldRequireOtpWhenUserIsSensitive() {
		UserAccountEntity user = new UserAccountEntity(
				"medecin@example.com",
				"Medecin Test",
				"MEDECIN",
				"hash");
		AuthenticationService service = service();

		when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

		LoginResponse response = service.login(new LoginRequest("medecin@example.com", "Password123!"), "127.0.0.1");

		assertEquals(true, response.requiresOtp());
		assertEquals(null, response.accessToken());
	}

	@Test
	void shouldAllowLoginAfterSuccessfulOtpVerification() {
		UserAccountEntity user = new UserAccountEntity(
				"medecin@example.com",
				"Medecin Test",
				"MEDECIN",
				"hash");
		AuthenticationService service = service();

		when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
		when(jwtService.createToken(user)).thenReturn(new JwtService.CreatedToken(
				"jwt-token",
				"token-id",
				Instant.parse("2026-07-02T10:30:00Z")));

		// Triggers OTP generation
		LoginResponse initialResponse = service.login(new LoginRequest("medecin@example.com", "Password123!"), "127.0.0.1");
		assertEquals(true, initialResponse.requiresOtp());

		String otpCode = service.getStaffOtpCodeForTesting("medecin@example.com");
		com.joprelys.backend.auth.api.VerifyStaffOtpRequest verifyRequest = new com.joprelys.backend.auth.api.VerifyStaffOtpRequest("medecin@example.com", otpCode);

		LoginResponse finalResponse = service.verifyStaffOtp(verifyRequest, "127.0.0.1");

		assertEquals("jwt-token", finalResponse.accessToken());
		assertEquals("medecin@example.com", finalResponse.email());
	}

	@Test
	void shouldFailOtpVerificationWhenOtpIsIncorrect() {
		UserAccountEntity user = new UserAccountEntity(
				"medecin@example.com",
				"Medecin Test",
				"MEDECIN",
				"hash");
		AuthenticationService service = service();

		when(userAccountRepository.findByEmail("medecin@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

		// Triggers OTP generation
		service.login(new LoginRequest("medecin@example.com", "Password123!"), "127.0.0.1");

		com.joprelys.backend.auth.api.VerifyStaffOtpRequest verifyRequest = new com.joprelys.backend.auth.api.VerifyStaffOtpRequest("medecin@example.com", "000000");

		assertThrows(
				BadCredentialsException.class,
				() -> service.verifyStaffOtp(verifyRequest, "127.0.0.1"));
	}

	private AuthenticationService service() {
		return new AuthenticationService(
				userAccountRepository,
				authAuditEventRepository,
				passwordEncoder,
				jwtService,
				jwtRevocationService,
				FIXED_CLOCK,
				organizationRepository);
	}
}
