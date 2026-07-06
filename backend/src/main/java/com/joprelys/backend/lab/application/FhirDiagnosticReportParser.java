package com.joprelys.backend.lab.application;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.joprelys.backend.lab.api.LabResultItem;
import com.joprelys.backend.lab.api.LabResultUploadRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Parseur de fichiers FHIR R4 DiagnosticReport vers LabResultUploadRequest.
 * Supporte JSON uniquement. STORY-1104.
 */
@Component
public class FhirDiagnosticReportParser {

    private final ObjectMapper objectMapper;

    public FhirDiagnosticReportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse un DiagnosticReport FHIR JSON vers un LabResultUploadRequest.
     *
     * @param fhirJson      le contenu JSON FHIR R4
     * @param validatorName nom du validateur (système ou personne)
     * @return un LabResultUploadRequest prêt pour l'ingestion
     */
    public LabResultUploadRequest parse(String fhirJson, String validatorName) {
        try {
            JsonNode root = objectMapper.readTree(fhirJson);

            validateResourceType(root);

            String examRequestNumber = extractExamRequestNumber(root);
            Instant resultAt = extractDateTime(root, "effectiveDateTime");
            Instant validatedAt = extractDateTime(root, "issued");
            List<LabResultItem> results = extractResults(root);

            if (results.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Format FHIR DiagnosticReport invalide : aucun résultat trouvé dans 'result'.");
            }

            String resolvedValidatorName = (validatorName != null && !validatorName.isBlank())
                    ? validatorName : "Système FHIR";

            return new LabResultUploadRequest(
                    examRequestNumber,
                    resolvedValidatorName,
                    null,          // validatorUserId
                    com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus.VALIDATED,
                    null,          // sampleCollectedAt non standard en FHIR DiagnosticReport
                    resultAt,
                    validatedAt,
                    null,          // conclusion non mappée depuis FHIR
                    results,
                    null           // pdfBase64 non fourni dans DiagnosticReport
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format FHIR DiagnosticReport invalide : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Méthodes privées d'extraction
    // -------------------------------------------------------------------------

    private void validateResourceType(JsonNode root) {
        JsonNode resourceType = root.get("resourceType");
        if (resourceType == null || !"DiagnosticReport".equals(resourceType.asText())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format FHIR invalide : resourceType doit être 'DiagnosticReport'.");
        }
    }

    private String extractExamRequestNumber(JsonNode root) {
        JsonNode subject = root.get("subject");
        if (subject == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format FHIR invalide : champ 'subject' manquant.");
        }
        JsonNode reference = subject.get("reference");
        if (reference == null || reference.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Format FHIR invalide : 'subject.reference' manquant.");
        }
        String ref = reference.asText();
        int slashIndex = ref.indexOf('/');
        return slashIndex >= 0 ? ref.substring(slashIndex + 1) : ref;
    }

    private Instant extractDateTime(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.asText().isBlank()) return null;
        try {
            return Instant.parse(node.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private List<LabResultItem> extractResults(JsonNode root) {
        JsonNode resultArray = root.get("result");
        List<LabResultItem> items = new ArrayList<>();
        if (resultArray == null || !resultArray.isArray()) return items;

        for (JsonNode obs : resultArray) {
            String analyteName = extractText(obs, "code");
            String value = extractQuantityValue(obs);
            String unit = extractQuantityUnit(obs);
            String referenceRange = extractReferenceRange(obs);
            String interpretation = extractInterpretation(obs);
            String comment = extractNote(obs);

            items.add(new LabResultItem(analyteName, value, unit, referenceRange, interpretation, comment));
        }
        return items;
    }

    private String extractText(JsonNode node, String field) {
        JsonNode f = node.get(field);
        if (f == null) return "Inconnu";
        JsonNode text = f.get("text");
        return text != null ? text.asText() : "Inconnu";
    }

    private String extractQuantityValue(JsonNode obs) {
        JsonNode vq = obs.get("valueQuantity");
        if (vq == null) return "N/A";
        JsonNode val = vq.get("value");
        return val != null ? val.asText() : "N/A";
    }

    private String extractQuantityUnit(JsonNode obs) {
        JsonNode vq = obs.get("valueQuantity");
        if (vq == null) return null;
        JsonNode unit = vq.get("unit");
        return unit != null ? unit.asText() : null;
    }

    private String extractReferenceRange(JsonNode obs) {
        JsonNode arr = obs.get("referenceRange");
        if (arr == null || !arr.isArray() || arr.isEmpty()) return null;
        JsonNode text = arr.get(0).get("text");
        return text != null ? text.asText() : null;
    }

    private String extractInterpretation(JsonNode obs) {
        JsonNode arr = obs.get("interpretation");
        if (arr == null || !arr.isArray() || arr.isEmpty()) return "NORMAL";
        JsonNode text = arr.get(0).get("text");
        return text != null ? text.asText() : "NORMAL";
    }

    private String extractNote(JsonNode obs) {
        JsonNode arr = obs.get("note");
        if (arr == null || !arr.isArray() || arr.isEmpty()) return null;
        JsonNode text = arr.get(0).get("text");
        return text != null ? text.asText() : null;
    }
}
