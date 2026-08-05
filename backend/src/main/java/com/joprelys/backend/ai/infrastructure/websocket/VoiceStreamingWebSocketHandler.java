package com.joprelys.backend.ai.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.ai.application.VoiceStreamingService;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;

/**
 * WebSocket handler pour le streaming vocal temps réel.
 *
 * <p>Protocole :</p>
 * <ul>
 *   <li>Client → Serveur : JSON {"type":"start", "visitId":"uuid", "locale":"fr"}</li>
 *   <li>Client → Serveur : JSON {"type":"audio", "data":"base64_audio_chunk"}</li>
 *   <li>Serveur → Client : JSON {"type":"transcript", "text":"...", "confidence":0.95, "isFinal":false}</li>
 *   <li>Client → Serveur : JSON {"type":"stop"}</li>
 * </ul>
 */
@Component
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
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            switch (type) {
                case "start" -> handleStart(session, payload);
                case "audio" -> handleAudio(session, payload);
                case "stop" -> handleStop(session);
                default -> {
                    log.warn("Type de message inconnu: {}", type);
                    sendError(session, "MESSAGE_TYPE_UNKNOWN");
                }
            }
        } catch (Exception e) {
            log.error("Erreur traitement message WebSocket", e);
            sendError(session, "MESSAGE_PROCESSING_ERROR");
        }
    }

    private void handleStart(WebSocketSession session, Map<String, Object> payload) {
        String visitIdStr = (String) payload.get("visitId");
        String locale = (String) payload.getOrDefault("locale", "fr");

        if (visitIdStr == null || visitIdStr.isBlank()) {
            sendError(session, "VISIT_ID_REQUIRED");
            return;
        }

        UUID visitId;
        try {
            visitId = UUID.fromString(visitIdStr);
        } catch (IllegalArgumentException e) {
            sendError(session, "VISIT_ID_INVALID");
            return;
        }

        // Vérifier autorisation : l'utilisateur peut-il accéder à cette visite ?
        UUID userId = (UUID) session.getAttributes().get("userId");
        UUID organizationId = (UUID) session.getAttributes().get("organizationId");

        if (userId == null || organizationId == null) {
            log.warn("Session WebSocket non authentifiée");
            sendError(session, "UNAUTHORIZED");
            try {
                session.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception e) {
                log.error("Erreur fermeture session", e);
            }
            return;
        }

        // TODO: Vérifier que userId a accès à visitId dans organizationId
        // Exemple : visitService.canAccess(visitId, userId, organizationId)

        log.info("Démarrage streaming: sessionId={}, visitId={}, locale={}, userId={}",
            session.getId(), visitId, locale, userId);

        // Démarrer le flux de transcription
        Disposable subscription = voiceStreamingService.startStreaming(visitId, locale)
            .subscribe(
                chunk -> sendTranscript(session, chunk),
                error -> {
                    log.error("Erreur flux transcription visitId={}", visitId, error);
                    sendError(session, "TRANSCRIPTION_ERROR");
                },
                () -> log.info("Flux transcription terminé visitId={}", visitId)
            );

        activeStreams.put(session.getId(), new StreamingContext(visitId, subscription));
        sendAck(session, "STREAMING_STARTED");
    }

    private void handleAudio(WebSocketSession session, Map<String, Object> payload) {
        StreamingContext context = activeStreams.get(session.getId());
        if (context == null) {
            sendError(session, "STREAM_NOT_STARTED");
            return;
        }

        String audioDataBase64 = (String) payload.get("data");
        if (audioDataBase64 == null || audioDataBase64.isBlank()) {
            log.warn("Chunk audio vide reçu");
            return;
        }

        try {
            byte[] audioChunk = Base64.getDecoder().decode(audioDataBase64);
            voiceStreamingService.sendAudioChunk(context.visitId(), audioChunk);
        } catch (IllegalArgumentException e) {
            log.error("Erreur décodage base64 audio", e);
            sendError(session, "AUDIO_DECODE_ERROR");
        }
    }

    private void handleStop(WebSocketSession session) {
        StreamingContext context = activeStreams.remove(session.getId());
        if (context != null) {
            voiceStreamingService.endStreaming(context.visitId());
            context.subscription().dispose();
            log.info("Streaming arrêté: sessionId={}, visitId={}", session.getId(), context.visitId());
            sendAck(session, "STREAMING_STOPPED");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket fermé: sessionId={}, status={}", session.getId(), status);
        StreamingContext context = activeStreams.remove(session.getId());
        if (context != null) {
            voiceStreamingService.endStreaming(context.visitId());
            context.subscription().dispose();
        }
    }

    private void sendTranscript(WebSocketSession session, VoiceStreamingService.TranscriptChunk chunk) {
        try {
            Map<String, Object> message = Map.of(
                "type", "transcript",
                "text", chunk.text(),
                "confidence", chunk.confidence(),
                "isFinal", chunk.isFinal()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error("Erreur envoi transcription", e);
        }
    }

    private void sendAck(WebSocketSession session, String status) {
        try {
            Map<String, Object> message = Map.of(
                "type", "ack",
                "status", status
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error("Erreur envoi ack", e);
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> message = Map.of(
                "type", "error",
                "error", error
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException e) {
            log.error("Erreur envoi erreur", e);
        }
    }

    private record StreamingContext(UUID visitId, Disposable subscription) {}
}
