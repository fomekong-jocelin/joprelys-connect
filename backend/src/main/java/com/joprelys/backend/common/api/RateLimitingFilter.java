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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Filtre de rate limiting simple par IP et par utilisateur authentifié.
 * Fenêtre glissante : N requêtes par fenêtre de T secondes.
 *
 * <p>L'adresse cliente est {@link HttpServletRequest#getRemoteAddr()} : derrière un proxy de
 * confiance, {@code server.forward-headers-strategy: native} la renseigne à partir de
 * {@code X-Forwarded-For} ; un en-tête fourni directement par le client n'est jamais lu, sans
 * quoi chacun pourrait changer de compartiment à volonté (SEC-05).</p>
 *
 * <p>Les compartiments inactifs sont purgés une fois par fenêtre et leur nombre est borné : au-delà
 * de la limite, les nouveaux clients sont servis sans suivi plutôt que de faire croître la mémoire.</p>
 *
 * Configuration via application.yml :
 * joprelys.rate-limiting.enabled: true
 * joprelys.rate-limiting.max-requests-per-window: 100
 * joprelys.rate-limiting.window-seconds: 60
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final String RATE_LIMITED_BODY =
            "{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"Trop de requêtes. Veuillez réessayer plus tard.\",\"trace_id\":\"%s\"}}";

    private final RateLimitingProperties properties;
    private final Clock clock;
    private final Map<String, RequestWindow> ipWindows = new ConcurrentHashMap<>();
    private final Map<String, RequestWindow> userWindows = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> nextPurgeAt;

    public RateLimitingFilter(RateLimitingProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.nextPurgeAt = new AtomicReference<>(clock.instant().plusSeconds(properties.getWindowSeconds()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Instant now = clock.instant();
        purgeIdleWindowsIfDue(now);
        RequestWindow window = resolveWindow(request);
        if (window != null && !window.tryAcquire(now, properties.getWindowSeconds(), properties.getMaxRequestsPerWindow())) {
            String traceId = TraceIdFilter.currentTraceIdOrUnknown();
            log.warn("[trace_id={}] Rate limit exceeded for client: {}", traceId, resolveClientKey(request));
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(RATE_LIMITED_BODY.formatted(traceId));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RequestWindow resolveWindow(HttpServletRequest request) {
        Map<String, RequestWindow> windows = request.getRemoteUser() != null ? userWindows : ipWindows;
        String clientKey = resolveClientKey(request);
        RequestWindow existing = windows.get(clientKey);
        if (existing != null) {
            return existing;
        }
        if (windows.size() >= MAX_TRACKED_CLIENTS) {
            log.warn("Rate limiting: capacité de suivi atteinte ({} clients), requête servie sans suivi.",
                    MAX_TRACKED_CLIENTS);
            return null;
        }
        return windows.computeIfAbsent(clientKey, key -> new RequestWindow());
    }

    private String resolveClientKey(HttpServletRequest request) {
        String user = request.getRemoteUser();
        if (user != null && !user.isBlank()) {
            return "user:" + user;
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void purgeIdleWindowsIfDue(Instant now) {
        Instant due = nextPurgeAt.get();
        if (now.isBefore(due) || !nextPurgeAt.compareAndSet(due, now.plusSeconds(properties.getWindowSeconds()))) {
            return;
        }
        Instant cutoff = now.minusSeconds(properties.getWindowSeconds());
        ipWindows.values().removeIf(window -> window.idleSince(cutoff));
        userWindows.values().removeIf(window -> window.idleSince(cutoff));
    }

    int trackedClients() {
        return ipWindows.size() + userWindows.size();
    }

    private static final class RequestWindow {
        private final Deque<Instant> timestamps = new ArrayDeque<>();

        synchronized boolean tryAcquire(Instant now, int windowSeconds, int maxRequests) {
            evictBefore(now.minusSeconds(windowSeconds));
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }

        synchronized boolean idleSince(Instant cutoff) {
            evictBefore(cutoff);
            return timestamps.isEmpty();
        }

        private void evictBefore(Instant cutoff) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.removeFirst();
            }
        }
    }
}
