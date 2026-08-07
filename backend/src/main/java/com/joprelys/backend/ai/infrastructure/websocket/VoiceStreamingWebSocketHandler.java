package com.joprelys.backend.ai.infrastructure.websocket;

import com.joprelys.backend.ai.application.VoiceStreamingService;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import tools.jackson.databind.ObjectMapper;

/**
 * Handler WebSocket de la dictée clinique cloud.
 *
 * <p>Le message {@code stop} déclenche d'abord la transcription du reliquat
 * audio. Le serveur envoie ensuite les derniers messages {@code transcript},
 * puis seulement l'accusé {@code STREAMING_STOPPED}.</p>
 */
@Component
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class VoiceStreamingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceStreamingWebSocketHandler.class);

    private final VoiceStreamingService voiceStreamingService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, StreamingContext> activeStreams;

    public VoiceStreamingWebSocketHandler(
            VoiceStreamingService voiceStreamingService,
            ObjectMapper objectMapper) {
        this.voiceStreamingService = voiceStreamingService;
        this.objectMapper = objectMapper;
        this.activeStreams = new ConcurrentHashMap<>();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connecté: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = payload.get("type") == null ? "" : payload.get("type").toString();

            switch (type) {
                case "start" -> handleStart(session, payload);
                case "audio" -> handleAudio(session, payload);
                case "stop" -> handleStop(session);
                case "ping" -> sendAck(session, "STREAMING_ALIVE");
                default -> {
                    log.warn("Type de message inconnu: {}", type);
                    sendError(session, "MESSAGE_TYPE_UNKNOWN");
                }
            }
        } catch (Exception exception) {
            log.error("Erreur traitement message WebSocket", exception);
            sendError(session, "MESSAGE_PROCESSING_ERROR");
        }
    }

    private void handleStart(WebSocketSession session, Map<String, Object> payload) {
        if (activeStreams.containsKey(session.getId())) {
            sendError(session, "STREAM_ALREADY_STARTED");
            return;
        }

        String visitIdValue = payload.get("visitId") == null
                ? null
                : payload.get("visitId").toString();
        String locale = payload.getOrDefault("locale", "fr").toString();

        if (visitIdValue == null || visitIdValue.isBlank()) {
            sendError(session, "VISIT_ID_REQUIRED");
            return;
        }

        UUID visitId;
        try {
            visitId = UUID.fromString(visitIdValue);
        } catch (IllegalArgumentException exception) {
            sendError(session, "VISIT_ID_INVALID");
            return;
        }

        UUID userId = (UUID) session.getAttributes().get("userId");
        UUID organizationId = (UUID) session.getAttributes().get("organizationId");
        if (userId == null || organizationId == null) {
            log.warn("Session WebSocket non authentifiée");
            sendError(session, "UNAUTHORIZED");
            closeSession(session, CloseStatus.POLICY_VIOLATION);
            return;
        }

        log.info(
                "Démarrage streaming sessionId={} visitId={} locale={} userId={}",
                session.getId(),
                visitId,
                locale,
                userId);

        Disposable subscription = voiceStreamingService.startStreaming(visitId, locale)
                .subscribe(
                        chunk -> sendTranscript(session, chunk),
                        error -> {
                            activeStreams.remove(session.getId());
                            log.error("Erreur flux transcription visitId={}", visitId, error);
                            sendError(session, "TRANSCRIPTION_ERROR");
                        },
                        () -> {
                            activeStreams.remove(session.getId());
                            log.info("Flux transcription terminé visitId={}", visitId);
                            if (session.isOpen()) {
                                sendAck(session, "STREAMING_STOPPED");
                            }
                        });

        activeStreams.put(session.getId(), new StreamingContext(visitId, subscription));
        sendAck(session, "STREAMING_STARTED");
    }

    private void handleAudio(WebSocketSession session, Map<String, Object> payload) {
        StreamingContext context = activeStreams.get(session.getId());
        if (context == null) {
            sendError(session, "STREAM_NOT_STARTED");
            return;
        }

        String audioDataBase64 = payload.get("data") == null
                ? null
                : payload.get("data").toString();
        if (audioDataBase64 == null || audioDataBase64.isBlank()) {
            log.warn("Chunk audio vide reçu");
            return;
        }

        try {
            byte[] audioChunk = Base64.getDecoder().decode(audioDataBase64);
            voiceStreamingService.sendAudioChunk(context.visitId(), audioChunk);
        } catch (IllegalArgumentException exception) {
            log.error("Erreur décodage base64 audio", exception);
            sendError(session, "AUDIO_DECODE_ERROR");
        }
    }

    private void handleStop(WebSocketSession session) {
        StreamingContext context = activeStreams.get(session.getId());
        if (context == null) {
            sendAck(session, "STREAMING_STOPPED");
            return;
        }

        log.info(
                "Finalisation streaming sessionId={} visitId={}",
                session.getId(),
                context.visitId());
        voiceStreamingService.endStreaming(context.visitId());
        // Ne pas disposer ici : la souscription doit transmettre le reliquat
        // transcrit, puis son callback de complétion enverra STREAMING_STOPPED.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket fermé: sessionId={}, status={}", session.getId(), status);
        StreamingContext context = activeStreams.remove(session.getId());
        if (context != null) {
            voiceStreamingService.cancelStreaming(context.visitId());
            context.subscription().dispose();
        }
    }

    private void sendTranscript(WebSocketSession session, VoiceStreamingService.TranscriptChunk chunk) {
        sendJson(session, Map.of(
                "type", "transcript",
                "text", chunk.text(),
                "confidence", chunk.confidence(),
                "isFinal", chunk.isFinal()));
    }

    private void sendAck(WebSocketSession session, String status) {
        sendJson(session, Map.of("type", "ack", "status", status));
    }

    private void sendError(WebSocketSession session, String error) {
        sendJson(session, Map.of("type", "error", "error", error));
    }

    private void sendJson(WebSocketSession session, Map<String, Object> payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(payload);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException exception) {
            log.error("Erreur envoi message WebSocket", exception);
        }
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException exception) {
            log.error("Erreur fermeture session WebSocket", exception);
        }
    }

    private record StreamingContext(UUID visitId, Disposable subscription) {
    }
}
