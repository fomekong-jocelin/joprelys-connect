package com.joprelys.backend.ai.ambient.application;

import java.util.List;

public interface AmbientDiarizationPort {

    DiarizedTranscript transcribe(byte[] audio, String contentType, String locale);

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
