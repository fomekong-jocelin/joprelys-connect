package com.joprelys.backend.ai.infrastructure.openai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utilitaire pour encoder des données audio PCM brut en format WAV.
 *
 * <p>Ajoute un header WAV standard aux données PCM pour compatibilité
 * avec OpenAI Whisper API et autres services de transcription.</p>
 */
public class WavEncoder {

    /**
     * Encode des données audio PCM 16-bit mono en fichier WAV.
     *
     * @param pcmData données audio PCM brut (16-bit little-endian)
     * @param sampleRate fréquence d'échantillonnage (ex: 16000)
     * @return données WAV complètes (header + PCM)
     */
    public static byte[] encodePcm16MonoToWav(byte[] pcmData, int sampleRate) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int numChannels = 1; // Mono
            int bitsPerSample = 16;
            int byteRate = sampleRate * numChannels * bitsPerSample / 8;
            int blockAlign = numChannels * bitsPerSample / 8;
            int dataSize = pcmData.length;
            int chunkSize = 36 + dataSize;

            ByteBuffer buffer = ByteBuffer.allocate(44);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // RIFF header
            buffer.put("RIFF".getBytes());
            buffer.putInt(chunkSize);
            buffer.put("WAVE".getBytes());

            // fmt sub-chunk
            buffer.put("fmt ".getBytes());
            buffer.putInt(16); // Sub-chunk size (PCM)
            buffer.putShort((short) 1); // Audio format (1 = PCM)
            buffer.putShort((short) numChannels);
            buffer.putInt(sampleRate);
            buffer.putInt(byteRate);
            buffer.putShort((short) blockAlign);
            buffer.putShort((short) bitsPerSample);

            // data sub-chunk
            buffer.put("data".getBytes());
            buffer.putInt(dataSize);

            out.write(buffer.array());
            out.write(pcmData);

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur encodage WAV", e);
        }
    }

    /**
     * Vérifie si les données audio ont déjà un header WAV.
     *
     * @param audioData données audio
     * @return true si header WAV présent
     */
    public static boolean hasWavHeader(byte[] audioData) {
        if (audioData == null || audioData.length < 12) {
            return false;
        }
        return audioData[0] == 'R'
                && audioData[1] == 'I'
                && audioData[2] == 'F'
                && audioData[3] == 'F'
                && audioData[8] == 'W'
                && audioData[9] == 'A'
                && audioData[10] == 'V'
                && audioData[11] == 'E';
    }
}
