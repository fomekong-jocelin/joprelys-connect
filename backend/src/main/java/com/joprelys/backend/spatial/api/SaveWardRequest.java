package com.joprelys.backend.spatial.api;

import com.joprelys.backend.spatial.domain.HospitalServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveWardRequest(
        @NotBlank(message = "Le nom du service est obligatoire.")
        @Size(max = 100, message = "Le nom du service ne peut pas dépasser 100 caractères.")
        String name,
        @NotNull(message = "Le type de service est obligatoire.")
        HospitalServiceType serviceType
) {
}
