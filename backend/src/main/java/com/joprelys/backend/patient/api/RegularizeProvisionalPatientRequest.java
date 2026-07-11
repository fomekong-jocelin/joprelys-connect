package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegularizeProvisionalPatientRequest(
        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(max = 180)
        String fullName,

        @NotBlank(message = "Le sexe est obligatoire.")
        @Size(max = 20)
        String gender,

        @NotNull(message = "La date de naissance est obligatoire.")
        LocalDate birthDate,

        @Size(max = 50)
        String phone,

        @NotBlank(message = "La ville est obligatoire.")
        @Size(max = 100)
        String city,

        @Size(max = 100)
        String district,

        @Size(max = 500)
        String address,

        @Size(max = 255)
        String email,

        @Size(max = 150)
        String emergencyContactName,

        @Size(max = 50)
        String emergencyContactPhone,

        @NotNull(message = "La source de l'identité est obligatoire.")
        IdentitySourceType sourceType,

        @NotBlank(message = "La preuve ou le contexte d'identification est obligatoire.")
        @Size(max = 500)
        String sourceDetails,

        @NotBlank(message = "Le motif de régularisation est obligatoire.")
        @Size(max = 500)
        String reason) {
}
