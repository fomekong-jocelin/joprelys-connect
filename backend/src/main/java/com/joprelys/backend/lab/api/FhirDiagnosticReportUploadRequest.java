package com.joprelys.backend.lab.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête d'import de résultats au format FHIR R4 DiagnosticReport.
 * STORY-1104.
 */
public record FhirDiagnosticReportUploadRequest(
        @NotBlank(message = "Le contenu FHIR JSON est obligatoire.")
        String fhirJson,
        String validatorName
) {}
