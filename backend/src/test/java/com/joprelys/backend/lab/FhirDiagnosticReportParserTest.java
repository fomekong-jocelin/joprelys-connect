package com.joprelys.backend.lab;

import tools.jackson.databind.ObjectMapper;
import com.joprelys.backend.lab.api.LabResultUploadRequest;
import com.joprelys.backend.lab.application.FhirDiagnosticReportParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests unitaires purs du FhirDiagnosticReportParser.
 * Aucun contexte Spring chargé. STORY-1104.
 */
class FhirDiagnosticReportParserTest {

    private FhirDiagnosticReportParser parser;

    @BeforeEach
    void setUp() {
        parser = new FhirDiagnosticReportParser(new ObjectMapper());
    }

    // -------------------------------------------------------------------------
    // 1. Payload valide complet
    // -------------------------------------------------------------------------

    @Test
    void parseShouldExtractAnalytesFromValidFhirJson() {
        String fhirJson = """
                {
                  "resourceType": "DiagnosticReport",
                  "subject": { "reference": "ServiceRequest/REQ-2024-001" },
                  "effectiveDateTime": "2024-06-15T08:00:00Z",
                  "issued": "2024-06-15T10:30:00Z",
                  "result": [
                    {
                      "code": { "text": "Hémoglobine" },
                      "valueQuantity": { "value": 14.2, "unit": "g/dL" },
                      "referenceRange": [ { "text": "12.0 - 16.0 g/dL" } ],
                      "interpretation": [ { "text": "NORMAL" } ],
                      "note": [ { "text": "Valeur dans la norme" } ]
                    },
                    {
                      "code": { "text": "Leucocytes" },
                      "valueQuantity": { "value": 9.8, "unit": "G/L" },
                      "referenceRange": [ { "text": "4.0 - 10.0 G/L" } ],
                      "interpretation": [ { "text": "NORMAL" } ]
                    }
                  ]
                }
                """;

        LabResultUploadRequest result = parser.parse(fhirJson, "Dr. Martin");

        assertThat(result).isNotNull();
        assertThat(result.examRequestNumber()).isEqualTo("REQ-2024-001");
        assertThat(result.validatorName()).isEqualTo("Dr. Martin");
        assertThat(result.results()).hasSize(2);
        assertThat(result.results().get(0).analyteName()).isEqualTo("Hémoglobine");
        assertThat(result.results().get(0).value()).isEqualTo("14.2");
        assertThat(result.results().get(0).unit()).isEqualTo("g/dL");
        assertThat(result.results().get(0).referenceRange()).isEqualTo("12.0 - 16.0 g/dL");
        assertThat(result.results().get(0).interpretation()).isEqualTo("NORMAL");
        assertThat(result.results().get(0).comment()).isEqualTo("Valeur dans la norme");
        assertThat(result.results().get(1).analyteName()).isEqualTo("Leucocytes");
        assertThat(result.pdfBase64()).isNull();
        assertThat(result.sampleCollectedAt()).isNull();
    }

    // -------------------------------------------------------------------------
    // 2. resourceType invalide → 400
    // -------------------------------------------------------------------------

    @Test
    void parseShouldThrowWhenResourceTypeIsInvalid() {
        String fhirJson = """
                {
                  "resourceType": "Patient",
                  "subject": { "reference": "ServiceRequest/REQ-2024-001" },
                  "result": [
                    { "code": { "text": "Test" }, "valueQuantity": { "value": 1.0, "unit": "mg" } }
                  ]
                }
                """;

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse(fhirJson, null)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("DiagnosticReport");
    }

    // -------------------------------------------------------------------------
    // 3. JSON malformé → 400
    // -------------------------------------------------------------------------

    @Test
    void parseShouldThrowWhenJsonIsMalformed() {
        String malformedJson = "{ resourceType: DiagnosticReport, INVALID }}}";

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse(malformedJson, null)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("Format FHIR DiagnosticReport invalide");
    }

    // -------------------------------------------------------------------------
    // 4. Tableau result vide → 400
    // -------------------------------------------------------------------------

    @Test
    void parseShouldThrowWhenNoResults() {
        String fhirJson = """
                {
                  "resourceType": "DiagnosticReport",
                  "subject": { "reference": "ServiceRequest/REQ-EMPTY" },
                  "result": []
                }
                """;

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> parser.parse(fhirJson, null)
        );

        assertThat(ex.getStatusCode().value()).isEqualTo(400);
        assertThat(ex.getReason()).contains("aucun résultat");
    }
}
