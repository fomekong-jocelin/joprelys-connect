package com.joprelys.backend.ai.application;

import com.joprelys.backend.ai.domain.AiTranscription;
import com.joprelys.backend.ai.infrastructure.openai.OpenAiStreamingTranscriptionService;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Service de streaming vocal temps réel pour la dictée clinique.
 *
 * <p>Gère les sessions de streaming audio par visitId, buffer les chunks audio
 * et coordonne la transcription via OpenAI Whisper.</p>
 */
@Service
public class VoiceStreamingService {

    private static final Logger log = LoggerFactory.getLogger(VoiceStreamingService.class);

    private static final int BUFFER_SIZE_SECONDS = 3;
    private static final int SAMPLE_RATE = 16000;
    private static final int BYTES_PER_SAMPLE = 2; // 16-bit PCM
    private static final int BUFFER_SIZE_BYTES = BUFFER_SIZE_SECONDS * SAMPLE_RATE * BYTES_PER_SAMPLE;

    private final OpenAiStreamingTranscriptionService transcriptionService;
    private final ConcurrentHashMap<UUID, StreamingSession> activeSessions;

    public VoiceStreamingService(OpenAiStreamingTranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
        this.activeSessions = new ConcurrentHashMap<>();
    }

    /**
     * Démarre une session de streaming vocal pour une visite.
     *
     * @param visitId identifiant de la visite
     * @param locale langue de transcription (fr, en)
     * @return flux de transcriptions temps réel
     */
    public Flux<TranscriptChunk> startStreaming(UUID visitId, String locale) {
        log.info("Démarrage streaming vocal pour visite={}, locale={}", visitId, locale);

        // Créer un sink pour recevoir les chunks audio
        Sinks.Many<byte[]> audioSink = Sinks.many().unicast().onBackpressureBuffer();

        StreamingSession session = new StreamingSession(
            visitId,
            locale,
            audioSink,
            new AudioBuffer(BUFFER_SIZE_BYTES)
        );

        activeSessions.put(visitId, session);

        // Stream des transcriptions
        return audioSink.asFlux()
            .buffer(BUFFER_SIZE_BYTES / 1024) // Buffer ~3 secondes
            .flatMap(chunks -> {
                byte[] audioChunk = concatenateChunks(chunks);
                return transcriptionService.transcribe(audioChunk, "audio/pcm", locale);
            })
            .map(transcription -> new TranscriptChunk(
                transcription.text(),
                transcription.confidence(),
                false // isFinal - sera implémenté avec la détection de silence
            ))
            .doOnTerminate(() -> endStreaming(visitId))
            .doOnError(error -> {
                log.error("Erreur streaming vocal visite={}", visitId, error);
                endStreaming(visitId);
            });
    }

    /**
     * Envoie un chunk audio à la session active.
     *
     * @param visitId identifiant de la visite
     * @param audioChunk données audio PCM 16-bit 16kHz mono
     */
    public void sendAudioChunk(UUID visitId, byte[] audioChunk) {
        StreamingSession session = activeSessions.get(visitId);
        if (session == null) {
            log.warn("Session streaming inexistante pour visite={}", visitId);
            return;
        }

        // Ajouter au buffer et émettre si plein
        session.audioBuffer().add(audioChunk);
        if (session.audioBuffer().isFull()) {
            byte[] bufferedAudio = session.audioBuffer().flush();
            session.audioSink().tryEmitNext(bufferedAudio);
        }
    }

    /**
     * Termine la session de streaming.
     */
    public void endStreaming(UUID visitId) {
        StreamingSession session = activeSessions.remove(visitId);
        if (session != null) {
            // Flush le buffer final
            byte[] remaining = session.audioBuffer().flush();
            if (remaining.length > 0) {
                session.audioSink().tryEmitNext(remaining);
            }
            session.audioSink().tryEmitComplete();
            log.info("Session streaming terminée pour visite={}", visitId);
        }
    }

    private byte[] concatenateChunks(java.util.List<byte[]> chunks) {
        int totalLength = chunks.stream().mapToInt(c -> c.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    private record StreamingSession(
        UUID visitId,
        String locale,
        Sinks.Many<byte[]> audioSink,
        AudioBuffer audioBuffer
    ) {}

    private static class AudioBuffer {
        private final byte[] buffer;
        private int position = 0;

        AudioBuffer(int capacity) {
            this.buffer = new byte[capacity];
        }

        void add(byte[] chunk) {
            int remaining = buffer.length - position;
            int toCopy = Math.min(chunk.length, remaining);
            System.arraycopy(chunk, 0, buffer, position, toCopy);
            position += toCopy;
        }

        boolean isFull() {
            return position >= buffer.length * 0.9; // 90% plein
        }

        byte[] flush() {
            byte[] result = new byte[position];
            System.arraycopy(buffer, 0, result, 0, position);
            position = 0;
            return result;
        }
    }

    public record TranscriptChunk(
        String text,
        Double confidence,
        boolean isFinal
    ) {}
}
