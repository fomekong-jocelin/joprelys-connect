package com.joprelys.backend.auth.security;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final BearerTokenResolver bearerTokenResolver;
	private final JwtService jwtService;
	private final JwtRevocationService jwtRevocationService;
	private final UserAccountRepository userAccountRepository;

	public JwtAuthenticationFilter(
			BearerTokenResolver bearerTokenResolver,
			JwtService jwtService,
			JwtRevocationService jwtRevocationService,
			UserAccountRepository userAccountRepository) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.jwtService = jwtService;
		this.jwtRevocationService = jwtRevocationService;
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		try {
			if (bearerTokenResolver.resolve(request).map(token -> authenticate(token, response)).orElse(true)) {
				filterChain.doFilter(request, response);
			}
		} finally {
			TenantContext.clear();
		}
	}

	private boolean authenticate(String token, HttpServletResponse response) {
		try {
			JwtClaims claims = jwtService.parseAndValidate(token);
			if (jwtRevocationService.isRevoked(claims.tokenId())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return false;
			}

			EffectiveIdentity identity = resolveEffectiveIdentity(claims);
			List<SimpleGrantedAuthority> authorities = Arrays.stream(identity.roles().split(","))
					.map(String::trim)
					.filter(role -> !role.isBlank())
					.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
					.toList();

			var authentication = new UsernamePasswordAuthenticationToken(identity.email(), token, authorities);
			authentication.setDetails(claims);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			if (identity.organizationId() != null) {
				TenantContext.setTenantId(identity.organizationId());
			}
			return true;
		} catch (InvalidTokenException | IllegalArgumentException exception) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
	}

	private EffectiveIdentity resolveEffectiveIdentity(JwtClaims claims) {
		if (isPatientOnly(claims.role())) {
			UUID organizationId = claims.organizationId() == null || claims.organizationId().isBlank()
					? null
					: UUID.fromString(claims.organizationId());
			return new EffectiveIdentity(claims.email(), "PATIENT", organizationId);
		}

		UserAccountEntity user = userAccountRepository.findByEmail(
				claims.email().trim().toLowerCase(Locale.ROOT))
				.filter(UserAccountEntity::isEnabled)
				.orElseThrow(() -> new InvalidTokenException("User is disabled or no longer exists"));
		if (!user.getId().toString().equals(claims.subject())) {
			throw new InvalidTokenException("Token subject does not match the current user");
		}
		return new EffectiveIdentity(user.getEmail(), user.getRole(), user.getOrganizationId());
	}

	private static boolean isPatientOnly(String roles) {
		if (roles == null) {
			return false;
		}
		List<String> normalizedRoles = Arrays.stream(roles.split(","))
				.map(String::trim)
				.filter(role -> !role.isBlank())
				.map(role -> role.toUpperCase(Locale.ROOT))
				.toList();
		return normalizedRoles.size() == 1 && "PATIENT".equals(normalizedRoles.getFirst());
	}

	private record EffectiveIdentity(String email, String roles, UUID organizationId) {
	}
}
