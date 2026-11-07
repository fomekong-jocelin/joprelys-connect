package com.joprelys.backend.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PatientAccessQrSignerTest {

    private static final String SECRET = "test-patient-qr-key-at-least-32-characters";
    private static final Instant VALID_UNTIL = Instant.parse("2026-11-07T10:15:00Z");

    private final PatientAccessQrSigner signer = new PatientAccessQrSigner(SECRET);

    @Test
    void acceptsAGenuineQrBeforeExpiry() {
        String sig = signer.sign("PAT-TEST-000001", VALID_UNTIL);

        assertThat(signer.payload("PAT-TEST-000001", VALID_UNTIL)).contains("\"sig\":\"" + sig + "\"");
        assertThat(signer.isValid("PAT-TEST-000001", VALID_UNTIL, sig, VALID_UNTIL.minusSeconds(60))).isTrue();
    }

    @Test
    void rejectsAnExpiredQr() {
        String sig = signer.sign("PAT-TEST-000001", VALID_UNTIL);

        assertThat(signer.isValid("PAT-TEST-000001", VALID_UNTIL, sig, VALID_UNTIL.plusSeconds(1))).isFalse();
    }

    @Test
    void rejectsAForgedPatientOrExtendedValidity() {
        String sig = signer.sign("PAT-TEST-000001", VALID_UNTIL);
        Instant now = VALID_UNTIL.minusSeconds(60);

        assertThat(signer.isValid("PAT-TEST-000002", VALID_UNTIL, sig, now)).isFalse();
        assertThat(signer.isValid("PAT-TEST-000001", VALID_UNTIL.plusSeconds(3600), sig, now)).isFalse();
        assertThat(signer.isValid("PAT-TEST-000001", VALID_UNTIL, null, now)).isFalse();
    }

    @Test
    void rejectsASignatureMadeWithAnotherKey() {
        String foreign = new PatientAccessQrSigner("another-patient-qr-key-of-32-characters!").sign("PAT-TEST-000001", VALID_UNTIL);

        assertThat(signer.isValid("PAT-TEST-000001", VALID_UNTIL, foreign, VALID_UNTIL.minusSeconds(60))).isFalse();
    }

    @Test
    void refusesToStartWithoutAStrongKey() {
        assertThatThrownBy(() -> new PatientAccessQrSigner("")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new PatientAccessQrSigner("trop-courte")).isInstanceOf(IllegalStateException.class);
    }
}
