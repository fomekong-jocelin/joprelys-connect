package com.joprelys.backend.prescription.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PrescriptionPinCipherTest {

    private static final String TEST_KEY = "test-prescription-pin-key-at-least-32-chars";

    private final PrescriptionPinCipher cipher = new PrescriptionPinCipher(TEST_KEY);

    @Test
    void roundTripsPinWithoutStoringItInClear() {
        String stored = cipher.encrypt("482913");

        assertThat(stored).startsWith(PrescriptionPinCipher.FORMAT_PREFIX).doesNotContain("482913");
        assertThat(cipher.decrypt(stored)).isEqualTo("482913");
    }

    @Test
    void usesRandomIvSoSamePinGivesDifferentCiphertexts() {
        assertThat(cipher.encrypt("123456")).isNotEqualTo(cipher.encrypt("123456"));
    }

    @Test
    void refusesValueEncryptedWithAnotherKey() {
        String stored = new PrescriptionPinCipher("another-test-key-also-at-least-32-chars!!").encrypt("123456");

        assertThatThrownBy(() -> cipher.decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesTamperedOrLegacyPlainValue() {
        String stored = cipher.encrypt("123456");
        String tampered = stored.substring(0, stored.length() - 2) + (stored.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt("1234")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handlesNullAndRefusesMissingOrShortKey() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThatThrownBy(() -> new PrescriptionPinCipher("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PrescriptionPinCipher("too-short")).isInstanceOf(IllegalStateException.class);
    }
}
