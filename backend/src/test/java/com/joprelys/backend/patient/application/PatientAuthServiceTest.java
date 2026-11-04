package com.joprelys.backend.patient.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.notification.application.AccountMailService;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PatientAuthServiceTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 5, 15);
    private static final String DPU = "DPU-JOP-000123";

    @Mock private PatientRepository patientRepository;
    @Mock private JwtService jwtService;
    @Mock private AccountMailService accountMailService;

    private PatientAuthService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-04T08:00:00Z"), ZoneOffset.UTC);
        service = new PatientAuthService(patientRepository, jwtService, accountMailService, clock);
    }

    @Test
    void everyRefusalReasonYieldsTheSameStatusAndMessage() {
        PatientEntity withEmail = patient("patient@example.test");
        PatientEntity withoutEmail = patient(null);
        when(patientRepository.findByGlobalPatientNumber("DPU-INCONNU")).thenReturn(Optional.empty());
        when(patientRepository.findByGlobalPatientNumber(DPU)).thenReturn(Optional.of(withEmail));
        when(patientRepository.findByGlobalPatientNumber("DPU-SANS-EMAIL")).thenReturn(Optional.of(withoutEmail));

        assertGenericRefusal(() -> service.generateAndSendOtp("DPU-INCONNU", "+237690000000", BIRTH_DATE));
        assertGenericRefusal(() -> service.generateAndSendOtp(DPU, "+237611111111", BIRTH_DATE));
        assertGenericRefusal(() -> service.generateAndSendOtp(DPU, "+237690000000", BIRTH_DATE.plusDays(1)));
        assertGenericRefusal(() -> service.generateAndSendOtp("DPU-SANS-EMAIL", "+237690000000", BIRTH_DATE));
        verify(accountMailService, never()).sendPatientLoginCode(anyString(), anyString(), anyString());
    }

    @Test
    void requestingANewCodeDoesNotResetFailedAttempts() {
        when(patientRepository.findByGlobalPatientNumber(DPU)).thenReturn(Optional.of(patient("patient@example.test")));

        String firstCode = service.generateAndSendOtp(DPU, "+237690000000", BIRTH_DATE);
        String wrongCode = wrongCodeFor(firstCode);
        assertBadRequest(() -> service.verifyOtp(DPU, wrongCode), "Code de sécurité incorrect.");
        assertBadRequest(() -> service.verifyOtp(DPU, wrongCode), "Code de sécurité incorrect.");

        String secondCode = service.generateAndSendOtp(DPU, "+237690000000", BIRTH_DATE);
        assertBadRequest(() -> service.verifyOtp(DPU, wrongCodeFor(secondCode)),
                "Trop de tentatives infructueuses. Veuillez régénérer un code.");

        assertThatThrownBy(() -> service.generateAndSendOtp(DPU, "+237690000000", BIRTH_DATE))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(exception.getReason()).isEqualTo(PatientAuthService.OTP_REQUEST_LOCKED);
                });
        assertThat(service.activeLoginCodeForTesting(DPU)).isEmpty();
    }

    @Test
    void lockedDossierStillAnswersTheGenericRefusalToAWrongIdentity() {
        when(patientRepository.findByGlobalPatientNumber(DPU)).thenReturn(Optional.of(patient("patient@example.test")));
        String code = service.generateAndSendOtp(DPU, "+237690000000", BIRTH_DATE);
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verifyOtp(DPU, wrongCodeFor(code)))
                    .isInstanceOf(ResponseStatusException.class);
        }

        assertGenericRefusal(() -> service.generateAndSendOtp(DPU, "+237611111111", BIRTH_DATE));
    }

    private static void assertGenericRefusal(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(exception.getReason()).isEqualTo(PatientAuthService.GENERIC_OTP_REQUEST_FAILURE);
        });
    }

    private static void assertBadRequest(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String reason) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(exception.getReason()).isEqualTo(reason);
        });
    }

    private static String wrongCodeFor(String code) {
        return "000000".equals(code) ? "111111" : "000000";
    }

    private static PatientEntity patient(String email) {
        PatientEntity patient = new PatientEntity(DPU, "PAT-1", "Patient Fictif", "FEMININ",
                BIRTH_DATE, "+237690000000", "Douala", "", "", "", "", "", "");
        patient.setEmail(email);
        return patient;
    }
}
