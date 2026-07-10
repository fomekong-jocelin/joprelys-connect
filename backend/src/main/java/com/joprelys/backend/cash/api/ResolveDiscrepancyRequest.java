package com.joprelys.backend.cash.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveDiscrepancyRequest(
    @NotBlank(message = "Les notes de résolution sont obligatoires")
    @Size(max = 1000, message = "Les notes ne doivent pas dépasser 1000 caractères")
    String resolutionNotes
) {}
