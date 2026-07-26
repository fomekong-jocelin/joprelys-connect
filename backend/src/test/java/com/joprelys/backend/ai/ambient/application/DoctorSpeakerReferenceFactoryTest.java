package com.joprelys.backend.ai.ambient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DoctorSpeakerReferenceFactoryTest {

    private final DoctorSpeakerReferenceFactory factory = new DoctorSpeakerReferenceFactory();

    @Test
    void shouldAcceptThreeSecondPcm16MonoWavAsDoctorReference() {
        byte[] wav = wavSeconds(3.0);

        var reference = factory.create(wav, "audio/wav");

        assertThat(reference.name()).isEqualTo("doctor");
        assertThat(reference.contentType()).isEqualTo("audio/wav");
        assertThat(reference.audio()).isEqualTo(wav);
        assertThat(reference.audio()).isNotSameAs(wav);
    }

    @Test
    void shouldRejectReferenceShorterThanTwoSeconds() {
        assertThatThrownBy(() -> factory.create(wavSeconds(1.5), "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_DOCTOR_REFERENCE_DURATION_INVALID");
    }

    @Test
    void shouldRejectReferenceLongerThanTenSeconds() {
        assertThatThrownBy(() -> factory.create(wavSeconds(10.5), "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_DOCTOR_REFERENCE_DURATION_INVALID");
    }

    @Test
    void shouldRejectNonWavReference() {
        assertThatThrownBy(() -> factory.create(new byte[100], "audio/webm"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_DOCTOR_REFERENCE_WAV_REQUIRED");
    }

    @Test
    void shouldRejectMalformedWavEvenWhenMimeTypeSaysWav() {
        assertThatThrownBy(() -> factory.create(new byte[128], "audio/wav"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("AI_AMBIENT_DOCTOR_REFERENCE_INVALID");
    }

    private byte[] wavSeconds(double seconds) {
        int sampleRate = 16_000;
        int channels = 1;
        int bitsPerSample = 16;
        int blockAlign = channels * bitsPerSample / 8;
        int byteRate = sampleRate * blockAlign;
        int dataSize = (int) Math.round(seconds * byteRate);
        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        ascii(buffer, "RIFF");
        buffer.putInt(36 + dataSize);
        ascii(buffer, "WAVE");
        ascii(buffer, "fmt ");
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        ascii(buffer, "data");
        buffer.putInt(dataSize);
        while (buffer.hasRemaining()) buffer.put((byte) 0);
        return buffer.array();
    }

    private void ascii(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(StandardCharsets.US_ASCII));
    }
}
