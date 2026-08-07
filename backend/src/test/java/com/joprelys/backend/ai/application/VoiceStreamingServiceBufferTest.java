package com.joprelys.backend.ai.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class VoiceStreamingServiceBufferTest {

    @Test
    void shouldRetainOverlapBetweenWindowsWithoutLosingFreshBytes() {
        VoiceStreamingService.AudioBuffer buffer = new VoiceStreamingService.AudioBuffer(10, 2);

        List<byte[]> first = buffer.add(sequence(0, 10));
        assertEquals(1, first.size());
        assertArrayEquals(sequence(0, 10), first.getFirst());

        List<byte[]> second = buffer.add(sequence(10, 8));
        assertEquals(1, second.size());
        assertArrayEquals(new byte[] {8, 9, 10, 11, 12, 13, 14, 15, 16, 17}, second.getFirst());
    }

    @Test
    void shouldNotFlushOverlapAlreadyTranscribed() {
        VoiceStreamingService.AudioBuffer buffer = new VoiceStreamingService.AudioBuffer(6, 2);

        assertEquals(1, buffer.add(sequence(0, 6)).size());
        assertArrayEquals(new byte[0], buffer.flush());
    }

    @Test
    void shouldFlushOverlapPlusOnlyTheNewTail() {
        VoiceStreamingService.AudioBuffer buffer = new VoiceStreamingService.AudioBuffer(6, 2);

        buffer.add(sequence(0, 6));
        buffer.add(new byte[] {6, 7});

        assertArrayEquals(new byte[] {4, 5, 6, 7}, buffer.flush());
    }

    private byte[] sequence(int start, int length) {
        byte[] result = new byte[length];
        for (int index = 0; index < length; index++) {
            result[index] = (byte) (start + index);
        }
        return result;
    }
}
