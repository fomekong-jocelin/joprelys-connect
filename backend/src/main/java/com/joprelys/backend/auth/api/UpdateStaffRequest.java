package com.joprelys.backend.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateStaffRequest(
        @NotBlank @Size(min = 3) String displayName,
        String role,
        List<String> roles,
        String photoPath,
        String signaturePath,
        String stampPath,
        String phone,
        String registrationNumber,
        String bio
) {
}
