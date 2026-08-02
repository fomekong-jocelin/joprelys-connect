package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private byte[] wav(boolean audible) {
        int sampleRate = 16_000;
        int samples = sampleRate;
        int dataSize = samples * 2;
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
        for (int index = 0; index < samples; index++) {
            short sample = audible
                    ? (short) (Math.sin(index * 2.0d * Math.PI * 220.0d / sampleRate) * 4000)
                    : 0;
            buffer.putShort(sample);
        }
        return buffer.array();
    }
}
