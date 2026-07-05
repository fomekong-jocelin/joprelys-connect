package com.joprelys.backend.auth.security;

import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationApiKeyEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationApiKeyRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	private final OrganizationApiKeyRepository apiKeyRepository;
	private final OrganizationRepository organizationRepository;

	public ApiKeyAuthenticationFilter(
			OrganizationApiKeyRepository apiKeyRepository,
			OrganizationRepository organizationRepository) {
		this.apiKeyRepository = apiKeyRepository;
		this.organizationRepository = organizationRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();
		if (path != null && path.startsWith("/api/public/")) {
			filterChain.doFilter(request, response);
			return;
		}

		String apiKey = request.getHeader("X-API-KEY");
		if (apiKey == null || apiKey.trim().isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			String hashedKey = hashKey(apiKey.trim());
			Optional<OrganizationApiKeyEntity> apiKeyOpt = apiKeyRepository.findByHashedKeyAndStatus(hashedKey, "ACTIVE");

			if (apiKeyOpt.isEmpty()) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Clé API invalide ou révoquée");
				return;
			}

			OrganizationApiKeyEntity keyEntity = apiKeyOpt.get();
			Optional<OrganizationEntity> orgOpt = organizationRepository.findById(keyEntity.getOrganizationId());

			if (orgOpt.isEmpty()) {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Organisation non trouvée");
				return;
			}

			OrganizationEntity org = orgOpt.get();
			if (!"ACTIVE".equalsIgnoreCase(org.getStatus())) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "L'organisation est désactivée");
				return;
			}

			if (!org.isApiEnabled()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "L'accès API de l'organisation est désactivé");
				return;
			}

			var authorities = List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"));
			var authentication = new UsernamePasswordAuthenticationToken(org.getEmail(), apiKey, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			TenantContext.setTenantId(org.getId());

			try {
				filterChain.doFilter(request, response);
			} finally {
				SecurityContextHolder.clearContext();
				TenantContext.clear();
			}
		} catch (Exception e) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erreur d'authentification par clé API");
		}
	}

	private String hashKey(String rawKey) {
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
			var hexString = new StringBuilder();
			for (byte b : hashBytes) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
