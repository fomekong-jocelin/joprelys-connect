package com.joprelys.backend.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre de rate limiting simple par IP et par utilisateur authentifié.
 * Utilise un bucket token algorithm simplifié : N requêtes par fenêtre de T secondes.
 *
 * Configuration via application.yml :
 * joprelys.rate-limiting.enabled: true
 * joprelys.rate-limiting.max-requests-per-window: 100
 * joprelys.rate-limiting.window-seconds: 60
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitingProperties properties;
    private final Map<String, RequestWindow> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, RequestWindow> userWindows = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request);
        Map<String, RequestWindow> windowMap = resolveWindowMap(request);

        RequestWindow window = windowMap.computeIfAbsent(clientKey, k -> new RequestWindow());
        synchronized (window) {
            window.cleanup(properties.getWindowSeconds());
            if (window.count >= properties.getMaxRequestsPerWindow()) {
                String traceId = org.slf4j.MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
                log.warn("[trace_id={}] Rate limit exceeded for client: {}", traceId, clientKey);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("""
                    {"error":{"code":"RATE_LIMITED","message":"Trop de requêtes. Veuillez réessayer plus tard.","trace_id":"%s"}}
                    """.formatted(traceId != null ? traceId : "trc_unknown"));
                return;
            }
            window.count++;
            window.timestamps.add(Instant.now());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String user = request.getRemoteUser();
        if (user != null && !user.isBlank()) {
            return "user:" + user;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private Map<String, RequestWindow> resolveWindowMap(HttpServletRequest request) {
        return request.getRemoteUser() != null ? userWindows : ipWindows;
    }

    private static class RequestWindow {
        int count = 0;
        final java.util.List<Instant> timestamps = new java.util.ArrayList<>();

        void cleanup(int windowSeconds) {
            Instant cutoff = Instant.now().minusSeconds(windowSeconds);
            timestamps.removeIf(t -> t.isBefore(cutoff));
            count = timestamps.size();
        }
    }
}
