package com.joprelys.backend.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filtre qui génère un trace_id unique pour chaque requête HTTP entrante.
 * Le trace_id est stocké dans un MDC (Mapped Diagnostic Context) pour être
 * inclus dans les logs et les réponses d'erreur.
 *
 * <p>Un {@code X-Trace-Id} fourni par le client n'est repris que s'il respecte
 * {@code [A-Za-z0-9_-]{1,64}} : toute autre valeur (retour à la ligne, guillemet, JSON…)
 * est remplacée par un identifiant généré, ce qui empêche l'injection dans les journaux et
 * dans les corps d'erreur (SEC-11). Le filtre s'exécute en premier pour que les refus
 * précoces (rate limiting, sécurité) portent déjà un trace_id.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    static final String UNKNOWN_TRACE_ID = "trc_unknown";
    private static final Pattern VALID_TRACE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!isValid(traceId)) {
            traceId = generateTraceId();
        }
        org.slf4j.MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /** Trace_id courant s'il est conforme, sinon {@value #UNKNOWN_TRACE_ID} : sûr à réinjecter dans du JSON. */
    static String currentTraceIdOrUnknown() {
        String traceId = org.slf4j.MDC.get(TRACE_ID_MDC_KEY);
        return isValid(traceId) ? traceId : UNKNOWN_TRACE_ID;
    }

    static boolean isValid(String traceId) {
        return traceId != null && VALID_TRACE_ID.matcher(traceId).matches();
    }

    private String generateTraceId() {
        return "trc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }
}
