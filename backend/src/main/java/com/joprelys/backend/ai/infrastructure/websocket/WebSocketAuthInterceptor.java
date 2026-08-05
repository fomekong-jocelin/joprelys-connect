package com.joprelys.backend.ai.infrastructure.websocket;

import com.joprelys.backend.auth.security.JwtClaims;
import com.joprelys.backend.auth.security.JwtService;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Intercepteur pour authentifier les connexions WebSocket via JWT.
 *
 * <p>Vérifie le token JWT dans le query parameter ou header Authorization,
 * valide les claims et injecte l'identité dans les attributs de session.</p>
 */
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        // Extraire le token JWT du query parameter ?token=... ou header Authorization
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake refusé : token JWT manquant");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            // Valider et parser le token
            JwtClaims claims = jwtService.parseAndValidate(token);
            if (claims == null) {
                log.warn("WebSocket handshake refusé : token JWT invalide");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Extraire userId et organizationId
            UUID userId = parseUuid(claims.subject());
            UUID organizationId = parseUuid(claims.organizationId());

            if (userId == null || organizationId == null) {
                log.warn("WebSocket handshake refusé : claims JWT incomplets");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            // Injecter dans les attributs de session WebSocket
            attributes.put("userId", userId);
            attributes.put("organizationId", organizationId);
            attributes.put("userEmail", claims.email());

            log.info("WebSocket authentifié : userId={}, org={}", userId, organizationId);
            return true;

        } catch (Exception e) {
            log.error("Erreur validation JWT WebSocket", e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Rien à faire après le handshake
    }

    private String extractToken(ServerHttpRequest request) {
        // 1. Chercher dans query parameter ?token=...
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6); // Après "token="
                }
            }
        }

        // 2. Chercher dans header Authorization: Bearer ...
        var authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }

    private UUID parseUuid(String value) {
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
