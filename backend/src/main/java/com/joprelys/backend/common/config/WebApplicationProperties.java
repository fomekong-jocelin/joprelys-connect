package com.joprelys.backend.common.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Public web application configuration used to build browser-facing links.
 *
 * @param baseUrl public origin of the Angular application
 */
@ConfigurationProperties(prefix = "joprelys.web")
public record WebApplicationProperties(URI baseUrl) {

    public WebApplicationProperties {
        if (baseUrl == null
                || baseUrl.getScheme() == null
                || baseUrl.getHost() == null
                || (!"https".equalsIgnoreCase(baseUrl.getScheme())
                && !"http".equalsIgnoreCase(baseUrl.getScheme()))) {
            throw new IllegalArgumentException(
                    "joprelys.web.base-url doit être une URL HTTP(S) absolue.");
        }
    }
}
