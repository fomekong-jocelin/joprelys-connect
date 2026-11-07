package com.joprelys.backend.eco.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.joprelys.backend.audit.infrastructure.persistence.AuditLogRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.eco.EcoContractSchemas;
import com.joprelys.backend.eco.EcoTestSecurityConfiguration;
import com.joprelys.backend.eco.EcoTestTokens;
import com.joprelys.backend.eco.domain.EcoStatus;
import com.joprelys.backend.eco.infrastructure.persistence.EcoPatientLinkRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * API écosystème v1 de bout en bout dans Spring : vrais jetons RS256 (clé de test), chaîne de sécurité dédiée,
 * persistance H2 (migrations Flyway), contrats eco-contracts. Données fictives uniquement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EcoTestSecurityConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EcoApiIntegrationTest {

    private static final String LINKS = "/api/eco/v1/patient-links";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EcoPatientLinkRepository linkRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private String dpu;
    private UUID patientId;

    @BeforeEach
    void createFictitiousPatient() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        dpu = "DPU-TEST-" + suffix;
        patientId = savePatient(dpu, suffix).getId();
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private PatientEntity savePatient(String patientDpu, String suffix) {
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Demo " + suffix, "demo-" + suffix.toLowerCase() + "@test.local", "600000000", "Littoral", "Douala"));
        TenantContext.setTenantId(organization.getId());
        try {
            return patientRepository.saveAndFlush(new PatientEntity(patientDpu, "PAT-" + suffix, "Patient Fictif",
                    "FEMININ", LocalDate.of(1990, 1, 1), null, "Douala", null, null, null, null, null, null));
        } finally {
            TenantContext.clear();
        }
    }

    // --- Liaison ---

    @Test
    void patientLinksProfileAndReceivesContractCompliantResources() throws Exception {
        UUID profile = UUID.randomUUID();
        String body = createLink(EcoTestTokens.patient(dpu), linkRequest(profile))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientLink.dpu").value(dpu))
                .andExpect(jsonPath("$.patientLink.issuer").value("joprelys:clinique-demo"))
                .andExpect(jsonPath("$.patientLink.status").value("active"))
                .andExpect(jsonPath("$.consent.grantee.reference").value("allopharma"))
                .andExpect(jsonPath("$.consent.scope[0]").value("prescriptions"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'none'")))
                .andReturn().getResponse().getContentAsString();

        assertThat(EcoContractSchemas.validate("patient-link.schema.json", json(body, "$.patientLink"))).isEmpty();
        assertThat(EcoContractSchemas.validate("consent.schema.json", json(body, "$.consent"))).isEmpty();
        assertThat(body).doesNotContain("Patient Fictif", "1990-01-01", "Douala");
        assertThat(auditLogRepository.findAll()).anyMatch(log ->
                patientId.equals(log.getPatientId()) && "ECO_PATIENT_LINK".equals(log.getResourceType()));
    }

    @Test
    void replayingTheSameLinkReturnsTheExistingOne() throws Exception {
        UUID profile = UUID.randomUUID();
        String first = createLink(EcoTestTokens.patient(dpu), linkRequest(profile))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = createLink(EcoTestTokens.patient(dpu), linkRequest(profile))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(second, "$.patientLink.id")).isEqualTo(JsonPath.read(first, "$.patientLink.id"));
    }

    @Test
    void profileAlreadyLinkedToAnotherDpuIsRejected() throws Exception {
        UUID profile = UUID.randomUUID();
        createLink(EcoTestTokens.patient(dpu), linkRequest(profile)).andExpect(status().isCreated());
        String otherDpu = "DPU-TEST-OTHER" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        savePatient(otherDpu, otherDpu.substring(9));
        createLink(EcoTestTokens.patient(otherDpu), linkRequest(profile))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROFILE_LINKED_ELSEWHERE"));
    }

    @Test
    void unknownDpuIsReportedWithoutDetail() throws Exception {
        createLink(EcoTestTokens.patient("DPU-TEST-INCONNU"), linkRequest(UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UNKNOWN_DPU"));
    }

    @Test
    void tokenWithoutDpuIsRejected() throws Exception {
        String token = EcoTestTokens.token(claims -> claims.claim("azp", "allopharma-mobile").claim("scope", "eco:patient-link"));
        createLink(token, linkRequest(UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MISSING_DPU"));
    }

    @Test
    void undeclaredClientIsRejected() throws Exception {
        String token = EcoTestTokens.token(claims -> claims.claim("azp", "inconnu").claim("scope", "eco:patient-link").claim("dpu", dpu));
        createLink(token, linkRequest(UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CLIENT"));
    }

    @Test
    void childRelationshipIsNotYetSupported() throws Exception {
        String request = linkRequest(UUID.randomUUID()).replace("\"self\"", "\"child\"");
        createLink(EcoTestTokens.patient(dpu), request)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_RELATIONSHIP"));
    }

    @Test
    void invalidBodyIsRejected() throws Exception {
        createLink(EcoTestTokens.patient(dpu), "{\"allopharmaProfileId\":\"" + UUID.randomUUID()
                + "\",\"relationship\":\"self\",\"scope\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        createLink(EcoTestTokens.patient(dpu), linkRequest(UUID.randomUUID()).replace("prescriptions", "dossier-complet"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void periodEndBeyondTwoYearsIsRejected() throws Exception {
        String request = linkRequest(UUID.randomUUID()).replace("}", ",\"periodEnd\":\""
                + Instant.now().plusSeconds(3L * 365 * 24 * 3600) + "\"}");
        createLink(EcoTestTokens.patient(dpu), request)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
    }

    // --- Sécurité des jetons ---

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post(LINKS).contentType(MediaType.APPLICATION_JSON).content(linkRequest(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutPatientLinkScopeIsForbidden() throws Exception {
        createLink(EcoTestTokens.server(), linkRequest(UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    void wrongAudienceWrongIssuerExpiredOrForeignSignatureAreUnauthorized() throws Exception {
        List<String> tokens = List.of(
                EcoTestTokens.token(claims -> patientClaims(claims).audience(List.of("autre-api"))),
                EcoTestTokens.token(claims -> patientClaims(claims).issuer("https://faux.example/realms/joprelys-id")),
                EcoTestTokens.token(claims -> patientClaims(claims)
                        .issuedAt(Instant.now().minusSeconds(3600)).expiresAt(Instant.now().minusSeconds(1800))),
                EcoTestTokens.signedByUnknownKey(this::patientClaims));
        for (String token : tokens) {
            createLink(token, linkRequest(UUID.randomUUID())).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void houseHs256TokenIsRefusedOnEcoApi() throws Exception {
        createLink(houseStyleToken(), linkRequest(UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void ecoTokenIsRefusedOnTheRestOfTheApi() throws Exception {
        mockMvc.perform(get("/api/patients").header("Authorization", "Bearer " + EcoTestTokens.patient(dpu)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void undeclaredEcoRouteIsDenied() throws Exception {
        mockMvc.perform(get(LINKS + "/" + UUID.randomUUID()).header("Authorization", "Bearer " + EcoTestTokens.server()))
                .andExpect(status().isForbidden());
    }

    // --- Révocation ---

    @Test
    void serverRevokesConsentIdempotently() throws Exception {
        String created = createLink(EcoTestTokens.patient(dpu), linkRequest(UUID.randomUUID()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String consentId = JsonPath.read(created, "$.consent.id");
        String linkId = JsonPath.read(created, "$.patientLink.id");

        String revoked = revoke(EcoTestTokens.server(), consentId, "{\"revokedBy\":\"patient\",\"reason\":\"Arrêt du partage\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.status").value("revoked"))
                .andExpect(jsonPath("$.consent.revocation.revokedBy").value("patient"))
                .andExpect(jsonPath("$.consent.meta.versionId").value(2))
                .andExpect(jsonPath("$.patientLink.status").value("revoked"))
                .andReturn().getResponse().getContentAsString();
        assertThat(EcoContractSchemas.validate("consent.schema.json", json(revoked, "$.consent"))).isEmpty();
        assertThat(EcoContractSchemas.validate("patient-link.schema.json", json(revoked, "$.patientLink"))).isEmpty();

        revoke(EcoTestTokens.server(), consentId, "{\"revokedBy\":\"patient\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.meta.versionId").value(2))
                .andExpect(jsonPath("$.consent.revocation.reason").value("Arrêt du partage"));
        assertThat(linkRepository.findById(UUID.fromString(linkId)).orElseThrow().getStatus()).isEqualTo(EcoStatus.REVOKED);
    }

    @Test
    void revokedLinkCanBeCreatedAgain() throws Exception {
        UUID profile = UUID.randomUUID();
        String created = createLink(EcoTestTokens.patient(dpu), linkRequest(profile)).andReturn().getResponse().getContentAsString();
        revoke(EcoTestTokens.server(), JsonPath.read(created, "$.consent.id"), "{\"revokedBy\":\"patient\"}").andExpect(status().isOk());
        createLink(EcoTestTokens.patient(dpu), linkRequest(profile)).andExpect(status().isCreated());
    }

    @Test
    void consentOfAnotherApplicationLooksMissing() throws Exception {
        revoke(EcoTestTokens.server(), UUID.randomUUID().toString(), "{\"revokedBy\":\"patient\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONSENT_NOT_FOUND"));
    }

    @Test
    void patientTokenCannotUseServerRevocation() throws Exception {
        revoke(EcoTestTokens.patient(dpu), UUID.randomUUID().toString(), "{\"revokedBy\":\"patient\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void systemRevocationIsNotAcceptedFromAnApplication() throws Exception {
        revoke(EcoTestTokens.server(), UUID.randomUUID().toString(), "{\"revokedBy\":\"system\"}")
                .andExpect(status().isBadRequest());
    }

    // --- Outils ---

    private ResultActions createLink(String token, String body) throws Exception {
        return mockMvc.perform(post(LINKS).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions revoke(String token, String consentId, String body) throws Exception {
        return mockMvc.perform(post("/api/eco/v1/consents/" + consentId + "/revoke").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String linkRequest(UUID profile) {
        return "{\"allopharmaProfileId\":\"" + profile + "\",\"relationship\":\"self\",\"scope\":[\"prescriptions\",\"lab-results\"]}";
    }

    private org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder patientClaims(
            org.springframework.security.oauth2.jwt.JwtClaimsSet.Builder claims) {
        return claims.claim("azp", "allopharma-mobile").claim("scope", "eco:patient-link").claim("dpu", dpu);
    }

    private static String json(String body, String path) {
        Object node = JsonPath.read(body, path);
        return com.jayway.jsonpath.internal.JsonFormatter.prettyPrint(JsonPath.parse(node).jsonString());
    }

    /** Jeton au format de l'authentification maison (HS256, secret partagé de test). */
    private static String houseStyleToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("joprelys-test").audience("joprelys-test").subject(UUID.randomUUID().toString())
                .claim("scope", "eco:patient-link").expirationTime(Date.from(Instant.now().plusSeconds(300))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner("test-jwt-secret-at-least-32-chars-long-hs256".getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
