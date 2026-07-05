package com.joprelys.backend.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
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

	public JwtAuthenticationFilter(
			BearerTokenResolver bearerTokenResolver,
			JwtService jwtService,
			JwtRevocationService jwtRevocationService) {
		this.bearerTokenResolver = bearerTokenResolver;
		this.jwtService = jwtService;
		this.jwtRevocationService = jwtRevocationService;
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
			List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(claims.role().split(","))
					.map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
					.toList();
			var authentication = new UsernamePasswordAuthenticationToken(claims.email(), token, authorities);
			authentication.setDetails(claims);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			if (claims.organizationId() != null && !claims.organizationId().isBlank()) {
				TenantContext.setTenantId(java.util.UUID.fromString(claims.organizationId()));
			}

			return true;
		} catch (InvalidTokenException exception) {
			SecurityContextHolder.clearContext();
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		}
	}
}
