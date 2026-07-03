package com.joprelys.backend.hospitalization.api;

import jakarta.validation.constraints.NotBlank;

public record CreateHospitalizationNoteRequest(
        @NotBlank(message = "Le contenu de la note est obligatoire.")
        String noteContent
) {}
