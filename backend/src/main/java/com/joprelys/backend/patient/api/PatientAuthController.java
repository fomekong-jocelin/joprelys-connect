package com.joprelys.backend.patient.api;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.patient.application.PatientAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/patient/auth")
public class PatientAuthController {

    private final PatientAuthService patientAuthService;

    public PatientAuthController(PatientAuthService patientAuthService) {
        this.patientAuthService = patientAuthService;
    }

    @PostMapping("/otp")
    public void requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        patientAuthService.generateAndSendOtp(
                request.globalPatientNumber(),
                request.phone(),
                request.birthDate()
        );
    }

    @PostMapping("/verify")
    public LoginResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return patientAuthService.verifyOtp(
                request.globalPatientNumber(),
                request.otpCode()
        );
    }
}
