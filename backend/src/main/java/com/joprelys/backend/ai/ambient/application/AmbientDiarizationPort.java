package com.joprelys.backend.ai.ambient.application;

import java.util.List;

public interface AmbientDiarizationPort {

    DiarizedTranscript transcribe(byte[] audio, String contentType, String locale);

    default DiarizedTranscript transcribe(
            byte[] audio,
            String contentType,
            String locale,
            List<KnownSpeakerReference> knownSpeakers) {
        if (knownSpeakers == null || knownSpeakers.isEmpty()) {
            return transcribe(audio, contentType, locale);
        }
        throw new UnsupportedOperationException("Known speaker references are not supported by this provider");
    }

    record KnownSpeakerReference(
            String name,
            byte[] audio,
            String contentType) {
    }

    record DiarizedTranscript(
            String text,
            List<DiarizedSegment> segments) {
    }

    record DiarizedSegment(
            String sourceSegmentId,
            double startSeconds,
            double endSeconds,
            String text,
            String speakerLabel) {
    }
}
