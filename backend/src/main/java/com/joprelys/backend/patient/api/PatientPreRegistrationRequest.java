package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record PatientPreRegistrationRequest(
        @NotNull(message = "L'établissement est obligatoire")
        UUID organizationId,

        @NotBlank(message = "Le prénom est obligatoire")
        @Size(min = 2, max = 100, message = "Le prénom doit avoir entre 2 et 100 caractères")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit avoir entre 2 et 100 caractères")
        String lastName,

        @NotBlank(message = "Le sexe est obligatoire")
        String gender,

        @NotNull(message = "La date de naissance est obligatoire")
        @PastOrPresent(message = "La date de naissance ne peut pas être dans le futur")
        LocalDate birthDate,

        String bloodGroup,
        String phone,
        String email,
        String address,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelation,

        @NotNull(message = "L'identifiant du captcha est obligatoire")
        UUID captchaId,

        @NotBlank(message = "La réponse au captcha est obligatoire")
        String captchaAnswer
) {}
