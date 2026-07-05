package com.joprelys.backend.patient.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MergePatientsRequest(
    @NotNull(message = "L'ID du patient principal est requis")
    UUID primaryId,
    @NotNull(message = "L'ID du patient secondaire est requis")
    UUID secondaryId
) {}