package com.joprelys.backend.patient.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.patient.application.PatientAccessGuardService;
import com.joprelys.backend.patient.application.PatientAccessQrSigner;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.visit.application.QrCodeGeneratorService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

class PatientAccessQrControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    private final PatientAccessGuardService guard = mock(PatientAccessGuardService.class);
    private final QrCodeGeneratorService generator = mock(QrCodeGeneratorService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final PatientAccessQrSigner signer = new PatientAccessQrSigner("test-patient-qr-key-at-least-32-characters");
    private final PatientAccessQrController controller =
            new PatientAccessQrController(guard, generator, signer, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void generatesQrOnServerWithServerSideValidity() {
        PatientEntity patient = mock(PatientEntity.class);
        when(patient.getGlobalPatientNumber()).thenReturn("PAT-TEST-000001");
        when(guard.resolve(authentication)).thenReturn(patient);
        when(generator.generateQrCode(org.mockito.ArgumentMatchers.anyString(), anyInt(), anyInt()))
                .thenReturn(new byte[] {1, 2, 3});

        var response = controller.getAccessQr(authentication);

        String expectedPayload = signer.payload("PAT-TEST-000001", Instant.parse("2026-11-07T10:15:00Z"));
        assertThat(expectedPayload).startsWith("{\"type\":\"JOPRELYS_PATIENT_ACCESS\",\"globalPatientNumber\":\"PAT-TEST-000001\","
                + "\"validUntil\":\"2026-11-07T10:15:00Z\",\"sig\":\"");
        verify(generator).generateQrCode(eq(expectedPayload), anyInt(), anyInt());
        assertThat(response.getBody()).containsExactly(1, 2, 3);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst(PatientAccessQrController.VALID_UNTIL_HEADER))
                .isEqualTo("2026-11-07T10:15:00Z");
    }
}
