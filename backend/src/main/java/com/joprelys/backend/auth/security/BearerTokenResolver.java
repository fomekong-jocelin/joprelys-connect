package com.joprelys.backend.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class BearerTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String VOICE_WEBSOCKET_PATH = "/api/voice/stream";
    private static final String WEBSOCKET_TOKEN_PARAMETER = "token";

    public Optional<String> resolve(HttpServletRequest request) {
        Optional<String> headerToken = resolveAuthorizationHeader(request);
        if (headerToken.isPresent()) {
            return headerToken;
        }

        if (!VOICE_WEBSOCKET_PATH.equals(request.getRequestURI())) {
            return Optional.empty();
        }

        String queryToken = request.getParameter(WEBSOCKET_TOKEN_PARAMETER);
        if (queryToken == null) {
            return Optional.empty();
        }
        String token = queryToken.trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }

    private Optional<String> resolveAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
