package com.joprelys.backend.ai.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.ai.domain.AiProvider;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "joprelys.ai.enabled=true",
        "joprelys.ai.openai.api-key=test-openai-key"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiConsultationIntegrationTest {

    private static final String TEST_JWT_SECRET =
            "test-jwt-secret-at-least-32-chars-long-hs256";

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @MockitoBean private AiProvider aiProvider;

    private UUID visitId;
    private UUID organizationId;
    private String doctorEmail;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique IA " + suffix,
                "ai-" + suffix + "@test.local",
                "690000001",
                "Centre",
                "Douala"));
        organizationId = organization.getId();

        TenantContext.setTenantId(organizationId);
        doctorEmail = "doctor-ai-" + suffix + "@test.local";
        UserAccountEntity doctor = new UserAccountEntity(
                doctorEmail,
                "Doctor IA",
                "MEDECIN",
                "password");
        doctor.setOrganizationId(organizationId);
        userAccountRepository.saveAndFlush(doctor);

        PatientEntity patient = new PatientEntity(
                "DPU-AI-" + suffix,
                "PAT-AI-" + suffix,
                "Patient IA",
                "MASCULIN",
                LocalDate.of(1980, 1, 1),
                "+237690000001",
                "Douala",
                "",
                "",
                "",
                "",
                "",
                "");
        patient = patientRepository.saveAndFlush(patient);
        VisitEntity visit = visitRepository.saveAndFlush(new VisitEntity(
                patient,
                "VIS-AI-" + suffix,
                "Test assistant vocal",
                "Médecine générale"));
        visitId = visit.getId();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldResolveLegacyStaffTokenAndReturnNoSessionWithoutServerError() throws Exception {
        String legacyToken = legacyStaffToken(doctorEmail.toUpperCase(), organizationId);

        mockMvc.perform(get("/api/ai/consultations/" + visitId + "/session")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldResolveLegacyStaffTokenWithRbacAndCreateSession() throws Exception {
        String legacyToken = legacyStaffToken(doctorEmail, organizationId);

        mockMvc.perform(post("/api/ai/consultations/" + visitId + "/sessions")
                        .header("Authorization", "Bearer " + legacyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draft\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/ai/consultations/" + visitId + "/session")
                        .header("Authorization", "Bearer " + legacyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitId").value(visitId.toString()));
    }

    private static String legacyStaffToken(String email, UUID organizationId) throws Exception {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String claims = "{"
                + "\"iss\":\"joprelys-test\","
                + "\"aud\":\"joprelys-test\","
                + "\"sub\":\"" + email + "\","
                + "\"email\":\"" + email + "\","
                + "\"name\":\"Doctor IA\","
                + "\"role\":\"MEDECIN\","
                + "\"org\":\"" + organizationId + "\","
                + "\"jti\":\"" + UUID.randomUUID() + "\","
                + "\"iat\":" + issuedAt.getEpochSecond() + ","
                + "\"exp\":" + expiresAt.getEpochSecond()
                + "}";
        String unsigned = base64Url(header) + "." + base64Url(claims);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        return unsigned + "." + signature;
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
