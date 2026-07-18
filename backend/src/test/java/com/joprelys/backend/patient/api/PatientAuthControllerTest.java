package com.joprelys.backend.patient.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.auth.api.RefreshTokenCookieManager;
import com.joprelys.backend.patient.application.PatientAuthService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientAuthControllerTest {

    @Mock
    private PatientAuthService patientAuthService;

    @Mock
    private RefreshTokenCookieManager cookieManager;

    @Mock
    private HttpServletResponse response;

    private PatientAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new PatientAuthController(patientAuthService, cookieManager);
    }

    @Test
    void shouldClearPreviousProfessionalCookieWhenPatientLoginSucceeds() {
        VerifyOtpRequest request = new VerifyOtpRequest("DPU-001", "123456");
        LoginResponse expected = new LoginResponse(
                "patient-token",
                "Bearer",
                Instant.parse("2099-01-01T00:00:00Z"),
                "DPU-001",
                "Patient",
                "PATIENT");
        when(patientAuthService.verifyOtp("DPU-001", "123456")).thenReturn(expected);

        LoginResponse actual = controller.verifyOtp(request, response);

        assertSame(expected, actual);
        verify(cookieManager).clear(response);
    }
}
