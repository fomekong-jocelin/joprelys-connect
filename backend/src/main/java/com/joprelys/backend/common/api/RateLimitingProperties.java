package com.joprelys.backend.common.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour le rate limiting.
 */
@ConfigurationProperties(prefix = "joprelys.rate-limiting")
public class RateLimitingProperties {

    private boolean enabled = true;
    private int maxRequestsPerWindow = 100;
    private int windowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRequestsPerWindow() {
        return maxRequestsPerWindow;
    }

    public void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}
