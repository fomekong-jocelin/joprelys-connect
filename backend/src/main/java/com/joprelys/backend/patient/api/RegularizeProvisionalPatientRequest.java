package com.joprelys.backend.patient.api;

import com.joprelys.backend.patient.domain.IdentitySourceType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegularizeProvisionalPatientRequest(
        @NotBlank(message = "PATIENT_FULL_NAME_REQUIRED")
        @Size(max = 180, message = "PATIENT_FULL_NAME_TOO_LONG")
        String fullName,

        @NotBlank(message = "PATIENT_GENDER_REQUIRED")
        @Size(max = 20, message = "PATIENT_GENDER_TOO_LONG")
        String gender,

        @NotNull(message = "PATIENT_BIRTH_DATE_REQUIRED")
        @PastOrPresent(message = "PATIENT_BIRTH_DATE_FUTURE")
        LocalDate birthDate,

        @Size(max = 50, message = "PATIENT_PHONE_TOO_LONG")
        String phone,

        @NotBlank(message = "PATIENT_CITY_REQUIRED")
        @Size(max = 100, message = "PATIENT_CITY_TOO_LONG")
        String city,

        @Size(max = 100, message = "PATIENT_DISTRICT_TOO_LONG")
        String district,

        @Size(max = 500, message = "PATIENT_ADDRESS_TOO_LONG")
        String address,

        @Email(message = "PATIENT_EMAIL_INVALID")
        @Size(max = 255, message = "PATIENT_EMAIL_TOO_LONG")
        String email,

        @Size(max = 150, message = "PATIENT_EMERGENCY_CONTACT_NAME_TOO_LONG")
        String emergencyContactName,

        @Size(max = 50, message = "PATIENT_EMERGENCY_CONTACT_PHONE_TOO_LONG")
        String emergencyContactPhone,

        @NotNull(message = "PATIENT_IDENTITY_SOURCE_REQUIRED")
        IdentitySourceType sourceType,

        @NotBlank(message = "PATIENT_IDENTITY_EVIDENCE_REQUIRED")
        @Size(max = 500, message = "PATIENT_IDENTITY_EVIDENCE_TOO_LONG")
        String sourceDetails,

        @NotBlank(message = "PATIENT_REGULARIZATION_REASON_REQUIRED")
        @Size(max = 500, message = "PATIENT_REGULARIZATION_REASON_TOO_LONG")
        String reason) {
}
