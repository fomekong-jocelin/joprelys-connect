package com.joprelys.backend.patient.api;

import com.joprelys.backend.auth.api.LoginResponse;
import com.joprelys.backend.patient.application.PatientAuthService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /**
     * STORY-1909 : Génère un OTP pour valider un consentement via OTP_SMS ou OTP_EMAIL.
     * Accessible publiquement (le patient n'est pas encore loggé quand il reçoit la notification).
     */
    @PostMapping("/consent-otp")
    public void requestConsentOtp(
            @RequestParam String globalPatientNumber,
            @RequestParam String consentId) {
        patientAuthService.generateConsentOtp(globalPatientNumber, consentId);
    }

    /**
     * STORY-1909 : Vérifie l'OTP de consentement.
     * Retourne 200 OK si le code est valide, 400 sinon.
     */
    @PostMapping("/consent-otp/verify")
    public void verifyConsentOtp(
            @RequestParam String globalPatientNumber,
            @RequestParam String consentId,
            @RequestParam String otpCode) {
        patientAuthService.verifyConsentOtp(globalPatientNumber, consentId, otpCode);
    }
}
