package com.joprelys.backend.ai.application;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AiConsultationInputValidator {

    static final int MAX_TRANSCRIPT_LENGTH = 12000;
    private static final int MAX_AUDIO_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "audio/webm",
            "audio/ogg",
            "audio/mp4",
            "audio/mpeg",
            "audio/wav",
            "application/octet-stream");

    private AiConsultationInputValidator() {
    }

    static void validateAudio(byte[] audio, String contentType) {
        if (audio == null || audio.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (audio.length > MAX_AUDIO_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "AI_AUDIO_TOO_LARGE");
        }
        if (!ALLOWED_MIME_TYPES.contains(normalizeMimeType(contentType))) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE, "AI_AUDIO_TYPE_UNSUPPORTED");
        }
    }

    static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "AI_MESSAGE_INVALID");
        }
        if (text.length() > MAX_TRANSCRIPT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "AI_TRANSCRIPT_TOO_LARGE");
        }
    }

    static String normalizeMimeType(String contentType) {
        return contentType == null
                ? ""
                : contentType.split(";", 2)[0].trim().toLowerCase();
    }
}
