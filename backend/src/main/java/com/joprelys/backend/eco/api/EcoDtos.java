package com.joprelys.backend.eco.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Corps de requête et ressources de l'API écosystème v1 (contrats eco-contracts v1, FHIR restreint). */
public final class EcoDtos {

    static final String SCOPE_PATTERN = "prescriptions|lab-results|immunizations|appointments|emergency-summary";

    private EcoDtos() {
    }

    public record CreatePatientLinkRequest(
            @NotNull UUID allopharmaProfileId,
            @NotNull @Pattern(regexp = "self|child|dependent-adult") String relationship,
            @NotEmpty @Size(max = 5) List<@NotNull @Pattern(regexp = SCOPE_PATTERN) String> scope,
            Instant periodEnd) {
    }

    public record RevokeConsentRequest(
            @NotNull @Pattern(regexp = "patient|guardian") String revokedBy,
            @Size(max = 200) String reason) {
    }

    public record PatientLinkResponse(PatientLinkResource patientLink, ConsentResource consent) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Meta(String source, int versionId, String lastUpdated) {
    }

    public record PatientLinkResource(
            String resourceType,
            String id,
            Meta meta,
            String allopharmaProfileId,
            String dpu,
            String issuer,
            String relationship,
            String consent,
            String status,
            String created) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConsentResource(
            String resourceType,
            String id,
            Meta meta,
            String status,
            List<String> scope,
            String patient,
            Grantee grantee,
            Period period,
            String dateTime,
            Revocation revocation) {
    }

    public record Grantee(String type, String reference) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Period(String start, String end) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Revocation(String revokedAt, String revokedBy, String reason) {
    }
}
