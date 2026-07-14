package com.joprelys.backend.auth.session.application;

public record SessionClientMetadata(String clientType, String userAgent, String networkPrefix) {

    public SessionClientMetadata {
        clientType = clean(clientType, "WEB");
        userAgent = clean(userAgent, null);
        networkPrefix = clean(networkPrefix, null);
    }

    private static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
