package com.joprelys.backend.ai.infrastructure.websocket;

import com.joprelys.backend.auth.security.JwtClaims;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Autorise le handshake du flux vocal temps réel (SEC-19).
 *
 * <p>Le handshake est une requête HTTP qui traverse la chaîne Spring Security :
 * {@code JwtAuthenticationFilter} y a déjà validé le jeton (signature, expiration, session
 * persistante non révoquée) et résolu les permissions RBAC depuis la base. L'intercepteur
 * s'appuie donc sur cette authentification — jamais sur un jeton re-parsé sans contrôle de
 * session — et exige une permission de saisie clinique, comme les endpoints IA REST
 * équivalents ({@code AiVoiceController}).</p>
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    static final Set<String> VOICE_STREAM_PERMISSIONS = Set.of("CLINICAL_WRITE", "VISIT_VITALS_WRITE");
    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof JwtClaims claims)) {
            log.warn("WebSocket handshake refusé : requête non authentifiée");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        UUID userId = parseUuid(claims.subject());
        UUID organizationId = parseUuid(claims.organizationId());
        if (userId == null || organizationId == null) {
            log.warn("WebSocket handshake refusé : identité ou établissement non résolus");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        if (!hasVoiceStreamPermission(authentication)) {
            log.warn("WebSocket handshake refusé : permission de saisie clinique absente (userId={})", userId);
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("userId", userId);
        attributes.put("organizationId", organizationId);
        attributes.put("userEmail", claims.email());
        log.info("WebSocket authentifié : userId={}, org={}", userId, organizationId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Aucun traitement après le handshake.
    }

    private static boolean hasVoiceStreamPermission(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(VOICE_STREAM_PERMISSIONS::contains);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
