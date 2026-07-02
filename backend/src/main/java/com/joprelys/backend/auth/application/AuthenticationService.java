package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";

	private final UserAccountRepository userAccountRepository;
	private final AuthAuditEventRepository authAuditEventRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JwtRevocationService jwtRevocationService;
	private final Clock clock;
	private final OrganizationRepository organizationRepository;

	public AuthenticationService(
			UserAccountRepository userAccountRepository,
			AuthAuditEventRepository authAuditEventRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			JwtRevocationService jwtRevocationService,
			Clock clock,
			OrganizationRepository organizationRepository) {
		this.userAccountRepository = userAccountRepository;
		this.authAuditEventRepository = authAuditEventRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.jwtRevocationService = jwtRevocationService;
		this.clock = clock;
		this.organizationRepository = organizationRepository;
	}

	@Transactional
	public LoginResponse login(LoginRequest request, String ipAddress) {
		String email = normalizeEmail(request.email());
		UserAccountEntity user = userAccountRepository.findByEmail(email)
				.filter(UserAccountEntity::isEnabled)
				.filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
				.orElse(null);

		if (user == null) {
			audit(email, ipAddress, false, "BAD_CREDENTIALS");
			throw new BadCredentialsException(GENERIC_LOGIN_FAILURE);
		}

		if (user.getOrganizationId() != null) {
			OrganizationEntity org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
			if (org != null && !"ACTIVE".equals(org.getStatus())) {
				audit(email, ipAddress, false, "INACTIVE_ORGANIZATION");
				throw new BadCredentialsException("Votre établissement est désactivé.");
			}
		}

		JwtService.CreatedToken token = jwtService.createToken(user);
		audit(email, ipAddress, true, null);
		return new LoginResponse(
				token.value(),
				"Bearer",
				token.expiresAt(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole());
	}

	public void logout(String token) {
		JwtClaims claims = jwtService.parseAndValidate(token);
		jwtRevocationService.revoke(claims.tokenId(), claims.expiresAt());
	}

	private void audit(String email, String ipAddress, boolean success, String failureReason) {
		authAuditEventRepository.save(new AuthAuditEventEntity(
				clock.instant(),
				email,
				ipAddress,
				success,
				failureReason));
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
