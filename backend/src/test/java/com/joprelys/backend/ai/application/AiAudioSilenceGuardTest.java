package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AiAudioSilenceGuardTest {

    @Test
    void shouldRejectSilentPcmWav() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> AiAudioSilenceGuard.rejectSilentPcmWav(wav(false), "audio/wav"));
        assertEquals("AI_AUDIO_SILENCE", exception.getReason());
    }

    @Test
    void shouldAcceptPcmWavContainingARealSignal() {
        assertDoesNotThrow(() ->
                AiAudioSilenceGuard.rejectSilentPcmWav(wav(true), "audio/wav"));
    }

    @Test
    void shouldDetectSilentRawPcmBeforeStreamingTranscription() {
        assertTrue(AiAudioSilenceGuard.isLikelySilentPcm16(pcm(false)));
    }

    @Test
    void shouldKeepAudibleRawPcmForStreamingTranscription() {
        assertFalse(AiAudioSilenceGuard.isLikelySilentPcm16(pcm(true)));
    }

    private byte[] wav(boolean audible) {
        byte[] pcm = pcm(audible);
        int sampleRate = 16_000;
        int dataSize = pcm.length;
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(new byte[] {'R', 'I', 'F', 'F'});
        buffer.putInt(36 + dataSize);
        buffer.put(new byte[] {'W', 'A', 'V', 'E'});
        buffer.put(new byte[] {'f', 'm', 't', ' '});
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.putInt(sampleRate);
        buffer.putInt(sampleRate * 2);
        buffer.putShort((short) 2);
        buffer.putShort((short) 16);
        buffer.put(new byte[] {'d', 'a', 't', 'a'});
        buffer.putInt(dataSize);
        buffer.put(pcm);
        return buffer.array();
    }

    private byte[] pcm(boolean audible) {
        int sampleRate = 16_000;
        ByteBuffer buffer = ByteBuffer.allocate(sampleRate * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int index = 0; index < sampleRate; index++) {
            short sample = audible
                    ? (short) (Math.sin(index * 2.0d * Math.PI * 220.0d / sampleRate) * 4000)
                    : 0;
            buffer.putShort(sample);
        }
        return buffer.array();
    }
}
