package com.joprelys.backend.prescription.api;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PharmacyValidationRequest(
        @NotBlank String prescriptionNumber,
        @NotBlank String pinCode,
        @NotBlank @Size(max = 2000) String reviewNotes) {}
