package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.infrastructure.openai.OpenAiStreamingTranscriptionService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service de streaming vocal temps réel pour la dictée clinique.
 *
 * <p>Le mobile transmet du PCM 16 bits, mono, 16 kHz. Le service regroupe
 * exactement trois secondes d'audio par appel de transcription, sans perdre
 * les octets excédentaires, et traite les fenêtres dans leur ordre d'arrivée.</p>
 */
@Service
@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")
public class VoiceStreamingService {

    private static final Logger log = LoggerFactory.getLogger(VoiceStreamingService.class);

    private static final int BUFFER_SIZE_SECONDS = 3;
    private static final int SAMPLE_RATE = 16_000;
    private static final int BYTES_PER_SAMPLE = 2;
    static final int TRANSCRIPTION_WINDOW_BYTES =
            BUFFER_SIZE_SECONDS * SAMPLE_RATE * BYTES_PER_SAMPLE;

    private final OpenAiStreamingTranscriptionService transcriptionService;
    private final ConcurrentHashMap<UUID, StreamingSession> activeSessions;

    public VoiceStreamingService(OpenAiStreamingTranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    /**
     * Démarre une session de streaming vocal pour une visite.
     *
     * <p>Une éventuelle ancienne session de la même visite est remplacée sans
     * pouvoir supprimer ultérieurement la nouvelle session lors de son cleanup.</p>
     */
    public Flux<TranscriptChunk> startStreaming(UUID visitId, String locale) {
        log.info("Démarrage streaming vocal pour visite={}, locale={}", visitId, locale);

        Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();
        StreamingSession session = new StreamingSession(
                visitId,
                locale,
                audioSink,
                new AudioBuffer(TRANSCRIPTION_WINDOW_BYTES));

        StreamingSession previous = activeSessions.put(visitId, session);
        if (previous != null) {
            log.warn("Remplacement d'une session vocale encore active pour visite={}", visitId);
            previous.cancel();
        }

        return audioSink.asFlux()
                .concatMap(audioChunk -> transcriptionService.transcribe(
                        audioChunk,
                        "audio/pcm",
                        locale))
                .filter(transcription -> transcription.text() != null
                        && !transcription.text().isBlank())
                .map(transcription -> new TranscriptChunk(
                        transcription.text(),
                        transcription.confidence(),
                        true))
                .doOnError(error -> log.error(
                        "Erreur streaming vocal visite={}", visitId, error))
                .doFinally(signal -> activeSessions.remove(visitId, session));
    }

    /**
     * Ajoute un paquet audio à la session active. Une entrée plus grande qu'une
     * fenêtre peut produire plusieurs fenêtres, sans troncature ni perte.
     */
    public void sendAudioChunk(UUID visitId, byte[] audioChunk) {
        if (audioChunk == null || audioChunk.length == 0) {
            return;
        }

        StreamingSession session = activeSessions.get(visitId);
        if (session == null) {
            log.warn("Session streaming inexistante pour visite={}", visitId);
            return;
        }

        session.accept(audioChunk);
    }

    /**
     * Termine proprement la session et envoie la dernière fenêtre incomplète à
     * la transcription avant de compléter le flux.
     */
    public void endStreaming(UUID visitId) {
        StreamingSession session = activeSessions.remove(visitId);
        if (session != null) {
            session.complete();
            log.info("Session streaming terminée pour visite={}", visitId);
        }
    }

    /**
     * Annule une session après rupture de la connexion cliente. Le reliquat est
     * abandonné puisqu'aucun client n'est encore présent pour recevoir le texte.
     */
    public void cancelStreaming(UUID visitId) {
        StreamingSession session = activeSessions.remove(visitId);
        if (session != null) {
            session.cancel();
            log.info("Session streaming annulée pour visite={}", visitId);
        }
    }

    private static final class StreamingSession {
        private final UUID visitId;
        private final String locale;
        private final Sinks.Many<byte[]> audioSink;
        private final AudioBuffer audioBuffer;
        private boolean closed;

        private StreamingSession(
                UUID visitId,
                String locale,
                Sinks.Many<byte[]> audioSink,
                AudioBuffer audioBuffer) {
            this.visitId = visitId;
            this.locale = locale;
            this.audioSink = audioSink;
            this.audioBuffer = audioBuffer;
        }

        private synchronized void accept(byte[] chunk) {
            if (closed) {
                return;
            }
            for (byte[] ready : audioBuffer.add(chunk)) {
                emit(ready);
            }
        }

        private synchronized void complete() {
            if (closed) {
                return;
            }
            closed = true;
            byte[] remaining = audioBuffer.flush();
            if (remaining.length > 0) {
                emit(remaining);
            }
            audioSink.tryEmitComplete();
        }

        private synchronized void cancel() {
            if (closed) {
                return;
            }
            closed = true;
            audioBuffer.clear();
            audioSink.tryEmitComplete();
        }

        private void emit(byte[] audio) {
            Sinks.EmitResult result = audioSink.tryEmitNext(audio);
            if (result.isFailure()) {
                log.warn(
                        "Échec émission audio visite={} locale={} résultat={}",
                        visitId,
                        locale,
                        result);
            }
        }
    }

    /**
     * Accumulateur borné conservant intégralement les paquets entrants.
     */
    static final class AudioBuffer {
        private final byte[] buffer;
        private int position;

        AudioBuffer(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.buffer = new byte[capacity];
        }

        synchronized List<byte[]> add(byte[] chunk) {
            List<byte[]> ready = new ArrayList<>();
            int sourceOffset = 0;

            while (sourceOffset < chunk.length) {
                int writable = buffer.length - position;
                int copied = Math.min(writable, chunk.length - sourceOffset);
                System.arraycopy(chunk, sourceOffset, buffer, position, copied);
                position += copied;
                sourceOffset += copied;

                if (position == buffer.length) {
                    ready.add(flush());
                }
            }

            return ready;
        }

        synchronized byte[] flush() {
            byte[] result = new byte[position];
            System.arraycopy(buffer, 0, result, 0, position);
            position = 0;
            return result;
        }

        synchronized void clear() {
            position = 0;
        }
    }

    public record TranscriptChunk(
            String text,
            Double confidence,
            boolean isFinal
    ) {
    }
}
