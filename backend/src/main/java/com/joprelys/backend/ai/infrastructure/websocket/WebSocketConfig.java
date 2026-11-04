package com.joprelys.backend.ai.infrastructure.websocket;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration WebSocket pour le streaming vocal temps réel.
 *
 * <p>Activé uniquement si joprelys.ai.enabled=true</p>
 *
 * <p>SEC-19 : les origines autorisées valent par défaut l'URL publique du front
 * ({@code joprelys.web.base-url}) ; le joker {@code *} est refusé au démarrage, car il
 * exposerait le flux vocal clinique à n'importe quel site (détournement de WebSocket).</p>
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceStreamingWebSocketHandler voiceStreamingHandler;
    private final WebSocketAuthInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            VoiceStreamingWebSocketHandler voiceStreamingHandler,
            WebSocketAuthInterceptor authInterceptor,
            @Value("${joprelys.cors.allowed-origins:${joprelys.web.base-url}}") String allowedOrigins) {
        this.voiceStreamingHandler = voiceStreamingHandler;
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = parseAllowedOrigins(allowedOrigins);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceStreamingHandler, "/api/voice/stream")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(allowedOrigins);
    }

    static String[] parseAllowedOrigins(String configured) {
        String[] origins = Arrays.stream(configured == null ? new String[0] : configured.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        if (origins.length == 0) {
            throw new IllegalStateException(
                    "joprelys.cors.allowed-origins (ou joprelys.web.base-url) doit lister au moins une origine.");
        }
        if (Arrays.stream(origins).anyMatch(origin -> origin.contains("*"))) {
            throw new IllegalStateException(
                    "joprelys.cors.allowed-origins ne doit pas contenir de joker « * » pour le flux vocal WebSocket.");
        }
        return origins;
    }
}
