package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.PasswordRecoveryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth/password-recovery")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    public PasswordRecoveryController(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @PostMapping("/request")
    public OtpResponse requestRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        String code = passwordRecoveryService.generateAndSendOtp(request.email());
        return new OtpResponse(code);
    }

    public record OtpResponse(String otpCode) {}

    @PostMapping("/reset")
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordRecoveryService.resetPassword(
                request.email(),
                request.otpCode(),
                request.newPassword()
        );
    }
}
