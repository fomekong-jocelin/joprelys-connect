package com.joprelys.backend.ai.ambient.application;

import com.joprelys.backend.ai.ambient.application.AmbientDiarizationPort.KnownSpeakerReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DoctorSpeakerReferenceFactory {

    private static final int MIN_WAV_HEADER_BYTES = 44;
    private static final int MAX_REFERENCE_BYTES = 2 * 1024 * 1024;
    private static final double MIN_DURATION_SECONDS = 2.0;
    private static final double MAX_DURATION_SECONDS = 10.0;

    public KnownSpeakerReference create(byte[] audio, String contentType) {
        String normalizedContentType = normalizeContentType(contentType);
        if (!normalizedContentType.equals("audio/wav") && !normalizedContentType.equals("audio/x-wav")) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "AI_AMBIENT_DOCTOR_REFERENCE_WAV_REQUIRED");
        }
        if (audio == null || audio.length < MIN_WAV_HEADER_BYTES || audio.length > MAX_REFERENCE_BYTES) {
            throw invalidReference();
        }
        double durationSeconds = pcmWavDurationSeconds(audio);
        if (durationSeconds < MIN_DURATION_SECONDS || durationSeconds > MAX_DURATION_SECONDS) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI_AMBIENT_DOCTOR_REFERENCE_DURATION_INVALID");
        }
        return new KnownSpeakerReference("doctor", audio.clone(), "audio/wav");
    }

    double pcmWavDurationSeconds(byte[] audio) {
        if (!ascii(audio, 0, 4).equals("RIFF") || !ascii(audio, 8, 4).equals("WAVE")) {
            throw invalidReference();
        }

        int offset = 12;
        long byteRate = -1;
        long dataSize = -1;
        while (offset + 8 <= audio.length) {
            String chunkId = ascii(audio, offset, 4);
            long chunkSize = unsignedIntLe(audio, offset + 4);
            long dataOffset = (long) offset + 8;
            long nextOffset = dataOffset + chunkSize + (chunkSize % 2);
            if (chunkSize < 0 || dataOffset > audio.length || dataOffset + chunkSize > audio.length) {
                throw invalidReference();
            }

            if (chunkId.equals("fmt ")) {
                if (chunkSize < 16) throw invalidReference();
                int audioFormat = unsignedShortLe(audio, (int) dataOffset);
                int channels = unsignedShortLe(audio, (int) dataOffset + 2);
                long sampleRate = unsignedIntLe(audio, (int) dataOffset + 4);
                byteRate = unsignedIntLe(audio, (int) dataOffset + 8);
                int blockAlign = unsignedShortLe(audio, (int) dataOffset + 12);
                int bitsPerSample = unsignedShortLe(audio, (int) dataOffset + 14);
                if (audioFormat != 1
                        || channels < 1 || channels > 2
                        || sampleRate < 8_000 || sampleRate > 96_000
                        || bitsPerSample != 16
                        || blockAlign <= 0
                        || byteRate != sampleRate * blockAlign) {
                    throw invalidReference();
                }
            } else if (chunkId.equals("data")) {
                dataSize = chunkSize;
            }

            if (nextOffset > Integer.MAX_VALUE) throw invalidReference();
            offset = (int) nextOffset;
        }

        if (byteRate <= 0 || dataSize <= 0) throw invalidReference();
        return (double) dataSize / (double) byteRate;
    }

    private int unsignedShortLe(byte[] bytes, int offset) {
        if (offset < 0 || offset + 2 > bytes.length) throw invalidReference();
        return Short.toUnsignedInt(ByteBuffer.wrap(bytes, offset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort());
    }

    private long unsignedIntLe(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) throw invalidReference();
        return Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt());
    }

    private String ascii(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > bytes.length) throw invalidReference();
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) return "";
        return contentType.trim().toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    }

    private ResponseStatusException invalidReference() {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "AI_AMBIENT_DOCTOR_REFERENCE_INVALID");
    }
}
