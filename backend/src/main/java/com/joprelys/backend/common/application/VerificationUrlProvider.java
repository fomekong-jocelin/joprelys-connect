package com.joprelys.backend.common.application;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

@Component
public class VerificationUrlProvider {

	@Value("${joprelys.documents.verification-base-url:https://joprelys.com/verify}")
	private String verificationBaseUrl;

	public String getVerificationUrl(String path) {
		String baseUrl = getDynamicVerificationBaseUrl();
		if (path == null || path.isBlank()) {
			return baseUrl;
		}
		if (path.startsWith("/")) {
			return baseUrl + path;
		}
		return baseUrl + "/" + path;
	}

	private String getDynamicVerificationBaseUrl() {
		try {
			RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
			if (attrs instanceof ServletRequestAttributes servletAttrs) {
				HttpServletRequest request = servletAttrs.getRequest();

				String scheme = request.getScheme();
				String serverName = request.getServerName();
				int serverPort = request.getServerPort();

				String forwardedProto = request.getHeader("X-Forwarded-Proto");
				if (forwardedProto != null && !forwardedProto.isBlank()) {
					scheme = forwardedProto.trim().toLowerCase(Locale.ROOT);
				}

				String forwardedHost = request.getHeader("X-Forwarded-Host");
				if (forwardedHost != null && !forwardedHost.isBlank()) {
					serverName = forwardedHost.trim();
					if (serverName.contains(":")) {
						String[] parts = serverName.split(":");
						serverName = parts[0];
						serverPort = Integer.parseInt(parts[1]);
					} else {
						serverPort = scheme.equalsIgnoreCase("https") ? 443 : 80;
					}
				}

				// Si l'API est sur un sous-domaine 'api.xxx', on redirige la vérification vers le domaine principal
				if (serverName.toLowerCase(Locale.ROOT).startsWith("api.")) {
					serverName = serverName.substring(4);
				}

				StringBuilder url = new StringBuilder();
				url.append(scheme).append("://").append(serverName);
				if (serverPort != 80 && serverPort != 443) {
					url.append(":").append(serverPort);
				}
				url.append("/verify");
				return url.toString();
			}
		} catch (Exception e) {
			// Fallback en cas d'erreur
		}
		return verificationBaseUrl;
	}
}
