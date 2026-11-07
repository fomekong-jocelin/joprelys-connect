package com.joprelys.backend.eco.api;

import jakarta.servlet.http.HttpServletRequest;

/** Chemins de l'API écosystème, protégés par les jetons Joprelys ID et non par l'authentification maison. */
public final class EcoApiPaths {

    public static final String PREFIX = "/api/eco/";
    public static final String V1 = "/api/eco/v1";
    public static final String PATTERN = "/api/eco/**";

    private EcoApiPaths() {
    }

    /** Vrai si la requête vise l'API écosystème ; les filtres maison doivent alors s'abstenir. */
    public static boolean isEcoRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;
        return path.startsWith(PREFIX);
    }
}
