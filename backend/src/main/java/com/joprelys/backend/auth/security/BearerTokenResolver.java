package com.joprelys.backend.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenResolver {

	private static final String BEARER_PREFIX = "Bearer ";

	public Optional<String> resolve(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return Optional.empty();
		}
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		return token.isEmpty() ? Optional.empty() : Optional.of(token);
	}
}
