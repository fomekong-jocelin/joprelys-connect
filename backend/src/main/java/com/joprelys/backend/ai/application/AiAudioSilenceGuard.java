package com.joprelys.backend.ai.application;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Conservative server-side guard for the PCM WAV produced by the mobile app. */
final class AiAudioSilenceGuard {

    private static final double MAX_SILENT_RMS = 180.0d;
    private static final double MIN_ACTIVE_SAMPLE_RATIO = 0.003d;
    private static final int ACTIVE_SAMPLE_THRESHOLD = 350;

    private AiAudioSilenceGuard() {
    }

    static void rejectSilentPcmWav(byte[] audio, String contentType) {
        if (!isWav(contentType) || audio == null || audio.length < 44) {
            return;
        }
        WavData wav = parse(audio);
        if (wav == null || wav.audioFormat != 1 || wav.bitsPerSample != 16 || wav.dataSize < 2) {
            return;
        }

        int sampleCount = wav.dataSize / 2;
        double sum = 0.0d;
        double sumSquares = 0.0d;
        for (int index = wav.dataOffset; index + 1 < wav.dataOffset + wav.dataSize; index += 2) {
            short sample = (short) ((audio[index] & 0xff) | (audio[index + 1] << 8));
            sum += sample;
            sumSquares += (double) sample * sample;
        }
        double mean = sum / sampleCount;
        double variance = Math.max(0.0d, (sumSquares / sampleCount) - (mean * mean));
        double rms = Math.sqrt(variance);

        int active = 0;
        for (int index = wav.dataOffset; index + 1 < wav.dataOffset + wav.dataSize; index += 2) {
            short sample = (short) ((audio[index] & 0xff) | (audio[index + 1] << 8));
            if (Math.abs(sample - mean) >= ACTIVE_SAMPLE_THRESHOLD) {
                active++;
            }
        }
        double activeRatio = (double) active / sampleCount;
        if (rms < MAX_SILENT_RMS && activeRatio < MIN_ACTIVE_SAMPLE_RATIO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI_AUDIO_SILENCE");
        }
    }

    private static boolean isWav(String contentType) {
        if (contentType == null) return false;
        String normalized = contentType.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
        return "audio/wav".equals(normalized) || "audio/x-wav".equals(normalized);
    }

    private static WavData parse(byte[] audio) {
        if (!fourCc(audio, 0, "RIFF") || !fourCc(audio, 8, "WAVE")) return null;
        int audioFormat = -1;
        int bitsPerSample = -1;
        int dataOffset = -1;
        int dataSize = -1;
        int offset = 12;
        while (offset + 8 <= audio.length) {
            int chunkSize = littleEndianInt(audio, offset + 4);
            if (chunkSize < 0 || offset + 8L + chunkSize > audio.length) return null;
            if (fourCc(audio, offset, "fmt ") && chunkSize >= 16) {
                audioFormat = littleEndianShort(audio, offset + 8);
                bitsPerSample = littleEndianShort(audio, offset + 22);
            } else if (fourCc(audio, offset, "data")) {
                dataOffset = offset + 8;
                dataSize = chunkSize;
                break;
            }
            offset += 8 + chunkSize + (chunkSize & 1);
        }
        return dataOffset < 0 ? null : new WavData(audioFormat, bitsPerSample, dataOffset, dataSize);
    }

    private static boolean fourCc(byte[] data, int offset, String expected) {
        if (offset < 0 || offset + 4 > data.length) return false;
        return expected.equals(new String(data, offset, 4, StandardCharsets.US_ASCII));
    }

    private static int littleEndianShort(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static int littleEndianInt(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }

    private record WavData(int audioFormat, int bitsPerSample, int dataOffset, int dataSize) {
    }
}
