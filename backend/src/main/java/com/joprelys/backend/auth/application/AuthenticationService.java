package com.joprelys.backend.auth.application;

import com.joprelys.backend.auth.api.LoginRequest;
import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.api.VerifyStaffOtpRequest;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventEntity;
import com.joprelys.backend.auth.infrastructure.persistence.AuthAuditEventRepository;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtRevocationService;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";
	private static final Set<String> SENSITIVE_ROLES = Set.of(
			"ADMIN_JOPRELYS",
			"ADMIN_CLINIQUE",
			"MEDECIN",
			"BIOLOGISTE",
			"PHARMACIEN"
	);

	private final UserAccountRepository userAccountRepository;
	private final AuthAuditEventRepository authAuditEventRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JwtRevocationService jwtRevocationService;
	private final Clock clock;
	private final OrganizationRepository organizationRepository;
	private final SecureRandom secureRandom;
	private final Map<String, StaffOtpData> staffOtpMap = new ConcurrentHashMap<>();

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
		this.secureRandom = new SecureRandom();
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

		// FR-USER-005 : les rôles sensibles nécessitent une authentification forte (OTP)
		// Check if any of the user's roles is sensitive
		boolean hasSensitiveRole = java.util.Arrays.stream(user.getRole().split(","))
				.map(String::trim)
				.anyMatch(SENSITIVE_ROLES::contains);

		if (hasSensitiveRole) {
			String code = String.format("%06d", secureRandom.nextInt(1000000));
			staffOtpMap.put(email, new StaffOtpData(code, email, clock.instant(), 0));
			// Simulation : envoi console
			System.out.println("[OTP STAFF] Code de connexion pour " + email + " : " + code);
			return new LoginResponse(
					null,
					null,
					null,
					user.getEmail(),
					user.getDisplayName(),
					user.getRole(),
					true
			);
		}

		// Update last login
		user.setLastLoginAt(clock.instant());
		userAccountRepository.save(user);

		JwtService.CreatedToken token = jwtService.createToken(user);
		audit(email, ipAddress, true, null);
		return new LoginResponse(
				token.value(),
				"Bearer",
				token.expiresAt(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				false);
	}

	@Transactional
	public LoginResponse verifyStaffOtp(VerifyStaffOtpRequest request, String ipAddress) {
		String email = normalizeEmail(request.email());
		StaffOtpData otpData = staffOtpMap.get(email);

		if (otpData == null) {
			throw new BadCredentialsException("Aucune demande de connexion active pour cet e-mail.");
		}

		if (otpData.isExpired(clock.instant())) {
			staffOtpMap.remove(email);
			throw new BadCredentialsException("Le code de sécurité a expiré.");
		}

		if (!otpData.code().equals(request.otpCode())) {
			int newAttempts = otpData.attempts() + 1;
			if (newAttempts >= 3) {
				staffOtpMap.remove(email);
				throw new BadCredentialsException("Trop de tentatives infructueuses. Veuillez régénérer un code.");
			} else {
				staffOtpMap.put(email, new StaffOtpData(otpData.code(), email, otpData.createdAt(), newAttempts));
				throw new BadCredentialsException("Code de sécurité incorrect.");
			}
		}

		staffOtpMap.remove(email);
		UserAccountEntity user = userAccountRepository.findByEmail(email)
				.filter(UserAccountEntity::isEnabled)
				.orElseThrow(() -> new BadCredentialsException("Utilisateur introuvable."));

		if (user.getOrganizationId() != null) {
			OrganizationEntity org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
			if (org != null && !"ACTIVE".equals(org.getStatus())) {
				throw new BadCredentialsException("Votre établissement est désactivé.");
			}
		}

		// Update last login
		user.setLastLoginAt(clock.instant());
		userAccountRepository.save(user);

		JwtService.CreatedToken token = jwtService.createToken(user);
		audit(email, ipAddress, true, null);
		return new LoginResponse(
				token.value(),
				"Bearer",
				token.expiresAt(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				false);
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

	String getStaffOtpCodeForTesting(String email) {
		StaffOtpData data = staffOtpMap.get(normalizeEmail(email));
		return data != null ? data.code() : null;
	}

	private record StaffOtpData(String code, String email, Instant createdAt, int attempts) {
		public boolean isExpired(Instant now) {
			return createdAt.plusSeconds(300).isBefore(now); // 5 minutes
		}
	}
}
