package com.joprelys.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DoctorUnifiedAdmissionRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private JwtService jwtService;
    @MockitoBean private AccountMailService accountMailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrganizationEntity organization;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique admission " + suffix,
                "admission-" + suffix + "@test.local",
                "6" + Math.abs(suffix.hashCode()),
                "Centre",
                "Douala"));

        TenantContext.setTenantId(organization.getId());
        UserAccountEntity doctor = new UserAccountEntity(
                "doctor-admission-" + suffix + "@test.local",
                "Dr Admission " + suffix,
                "MEDECIN",
                "password");
        doctor.setOrganizationId(organization.getId());
        doctor = userAccountRepository.saveAndFlush(doctor);
        doctorToken = jwtService.createToken(doctor).value();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void doctorCanCreateNewPatientThenNormalVisit() throws Exception {
        mockMvc.perform(get("/api/rbac/me")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@ == 'MEDECIN')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'PATIENT_WRITE')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'VISIT_CREATE')]").exists());

        UUID patientId = createPatient("Normal");

        mockMvc.perform(post("/api/visits")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "reason", "Mal de tête",
                                "orientation", "CONSULTATION",
                                "service", "Médecine générale",
                                "arrivalAt", Instant.now().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    @Test
    void doctorCanCreateNewPatientThenEmergencyAdmission() throws Exception {
        UUID patientId = createPatient("Urgence");

        mockMvc.perform(post("/api/emergencies")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "arrivalMode", "WALK_IN",
                                "triageLevel", "GREEN",
                                "hemodynamicStatus", "STABLE",
                                "chiefComplaint", "Céphalée aiguë",
                                "initialBpSystolic", 120,
                                "initialBpDiastolic", 80,
                                "initialHr", 75,
                                "initialTemp", 37.0,
                                "thirdPartyConsentToContact", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientId.toString()));
    }

    private UUID createPatient(String scenario) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        MvcResult result = mockMvc.perform(post("/api/patients")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Patient " + scenario + " " + suffix,
                                "gender", "MASCULIN",
                                "birthDate", LocalDate.of(1990, 1, 1).toString(),
                                "phone", "+23769" + Math.abs(suffix.hashCode()),
                                "city", "Douala"))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("id").asText());
    }
}
