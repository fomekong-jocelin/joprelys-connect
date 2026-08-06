package com.joprelys.backend.ai.infrastructure.websocket;

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
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceStreamingWebSocketHandler voiceStreamingHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${joprelys.cors.allowed-origins:*}")
    private String allowedOrigins;

    public WebSocketConfig(
            VoiceStreamingWebSocketHandler voiceStreamingHandler,
            WebSocketAuthInterceptor authInterceptor) {
        this.voiceStreamingHandler = voiceStreamingHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceStreamingHandler, "/api/voice/stream")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
