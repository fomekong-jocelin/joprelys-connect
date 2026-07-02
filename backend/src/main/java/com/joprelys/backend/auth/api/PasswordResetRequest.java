package com.joprelys.backend.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank(message = "L'adresse e-mail est obligatoire.")
    @Email(message = "L'adresse e-mail doit être valide.")
    String email,

    @NotBlank(message = "Le code OTP est obligatoire.")
    String otpCode,

    @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
    @Size(min = 8, message = "Le nouveau mot de passe doit faire au moins 8 caractères.")
    String newPassword
) {}
