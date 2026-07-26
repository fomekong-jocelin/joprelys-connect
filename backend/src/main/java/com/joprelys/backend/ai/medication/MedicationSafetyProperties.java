package com.joprelys.backend.ai.medication;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "joprelys.ai.medication-safety")
public record MedicationSafetyProperties(
        boolean enabled,
        boolean failClosed,
        String knowledgeProvider,
        String interactionProvider,
        RxNormProperties rxnorm) {

    public record RxNormProperties(
            String baseUrl,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            int cacheMinutes) {
    }
}
