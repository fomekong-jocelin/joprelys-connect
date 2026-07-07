package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientPreRegistrationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PreRegistrationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.time.LocalDate;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PatientPreRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PatientPreRegistrationRepository preRegistrationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private OrganizationEntity orgA;
    private UserAccountEntity userAgentA;
    private String tokenAgentA;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM patient_pre_registrations");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();
        jdbcTemplate.update("SET REFERENTIAL_INTEGRITY TRUE");

        // Create Organization
        orgA = new OrganizationEntity("Clinique Test A", "contacta@joprelys.local", "123", "Street A", "Douala");
        orgA = organizationRepository.save(orgA);

        TenantContext.setTenantId(orgA.getId());

        // Create User
        userAgentA = new UserAccountEntity("agent.a@joprelys.local", "Agent A", "AGENT_ACCUEIL", "passhash");
        userAgentA.setOrganizationId(orgA.getId());
        userAgentA = userAccountRepository.save(userAgentA);

        // Token
        tokenAgentA = jwtService.createToken(userAgentA).value();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testCaptchaFlowAndSelfRegistration() throws Exception {
        // 1. Get Captcha
        MvcResult captchaResult = mockMvc.perform(get("/api/public/pre-registrations/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaId").exists())
                .andExpect(jsonPath("$.question").exists())
                .andReturn();

        String responseBody = captchaResult.getResponse().getContentAsString();
        MedicalCaptchaResponse captcha = objectMapper.readValue(responseBody, MedicalCaptchaResponse.class);

        // Determine answer based on question text mapping
        String answer = "37"; // default fallback
        String questionLower = captcha.question().toLowerCase();
        if (questionLower.contains("cel") || questionLower.contains("temp")) {
            answer = "37";
        } else if (questionLower.contains("pomp")) {
            answer = "coeur";
        } else if (questionLower.contains("doigt")) {
            answer = "5";
        } else if (questionLower.contains("liquide rouge") || questionLower.contains("veines")) {
            answer = "sang";
        } else if (questionLower.contains("squelette") || questionLower.contains("os")) {
            answer = "os";
        } else if (questionLower.contains("nerveux") || questionLower.contains("central")) {
            answer = "cerveau";
        } else if (questionLower.contains("poumon") && questionLower.contains("combien")) {
            answer = "2";
        } else if (questionLower.contains("respir")) {
            answer = "poumons";
        }

        // 2. Submit Pre-Registration (Invalid Captcha)
        String invalidRequest = String.format("""
                {
                    "organizationId": "%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "gender": "MASCULIN",
                    "birthDate": "1990-01-15",
                    "captchaId": "%s",
                    "captchaAnswer": "incorrect_answer"
                }
                """, orgA.getId(), captcha.captchaId());

        mockMvc.perform(post("/api/public/pre-registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // 3. Get a NEW Captcha for the valid submission since the previous one was consumed
        MvcResult captchaResult2 = mockMvc.perform(get("/api/public/pre-registrations/captcha"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody2 = captchaResult2.getResponse().getContentAsString();
        MedicalCaptchaResponse captcha2 = objectMapper.readValue(responseBody2, MedicalCaptchaResponse.class);

        // Determine answer for the second captcha
        String answer2 = "37";
        String questionLower2 = captcha2.question().toLowerCase();
        if (questionLower2.contains("cel") || questionLower2.contains("temp")) {
            answer2 = "37";
        } else if (questionLower2.contains("pomp")) {
            answer2 = "coeur";
        } else if (questionLower2.contains("doigt")) {
            answer2 = "5";
        } else if (questionLower2.contains("liquide rouge") || questionLower2.contains("veines")) {
            answer2 = "sang";
        } else if (questionLower2.contains("squelette") || questionLower2.contains("os")) {
            answer2 = "os";
        } else if (questionLower2.contains("nerveux") || questionLower2.contains("central")) {
            answer2 = "cerveau";
        } else if (questionLower2.contains("poumon") && questionLower2.contains("combien")) {
            answer2 = "2";
        } else if (questionLower2.contains("respir")) {
            answer2 = "poumons";
        }

        // 4. Submit Pre-Registration (Valid Captcha)
        String validRequest = String.format("""
                {
                    "organizationId": "%s",
                    "firstName": "John",
                    "lastName": "Doe",
                    "gender": "MASCULIN",
                    "birthDate": "1990-01-15",
                    "phone": "+237677123456",
                    "email": "john.doe@example.com",
                    "address": "Yaoundé, Cameroun",
                    "emergencyContactName": "Jane Doe",
                    "emergencyContactPhone": "+237699123456",
                    "emergencyContactRelation": "SPOUSE",
                    "captchaId": "%s",
                    "captchaAnswer": "%s"
                }
                """, orgA.getId(), captcha2.captchaId(), answer2);

        MvcResult submissionResult = mockMvc.perform(post("/api/public/pre-registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("AWAITING_VALIDATION"))
                .andReturn();

        String submissionResponse = submissionResult.getResponse().getContentAsString();
        PatientPreRegistrationResponse preReg = objectMapper.readValue(submissionResponse, PatientPreRegistrationResponse.class);

        // 4. Get Pending Pre-Registrations (Secure)
        mockMvc.perform(get("/api/pre-registrations")
                .header("Authorization", "Bearer " + tokenAgentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(preReg.id().toString()))
                .andExpect(jsonPath("$.content[0].firstName").value("John"))
                .andExpect(jsonPath("$.content[0].lastName").value("Doe"));

        // 5. Get Details By ID
        mockMvc.perform(get("/api/pre-registrations/" + preReg.id())
                .header("Authorization", "Bearer " + tokenAgentA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));

        // 6. Validate Pre-Registration (Create new patient)
        String validationRequest = """
                {
                    "firstName": "John",
                    "lastName": "Doe",
                    "gender": "MASCULIN",
                    "birthDate": "1990-01-15",
                    "phone": "+237677123456",
                    "email": "john.doe@example.com",
                    "address": "Yaoundé, Cameroun",
                    "emergencyContactName": "Jane Doe",
                    "emergencyContactPhone": "+237699123456",
                    "emergencyContactRelation": "SPOUSE"
                }
                """;

        mockMvc.perform(post("/api/pre-registrations/" + preReg.id() + "/validate")
                .header("Authorization", "Bearer " + tokenAgentA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validationRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").exists())
                .andExpect(jsonPath("$.status").value("VALIDATED"));

        // Verify patient is actually created in DPU
        mockMvc.perform(get("/api/patients")
                .header("Authorization", "Bearer " + tokenAgentA)
                .param("q", "Doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("DOE John"));
    }

    @Test
    void testRejectPreRegistration() throws Exception {
        TenantContext.setTenantId(orgA.getId());

        // Create pre-registration directly in DB for testing rejection
        var entity = new com.joprelys.backend.patient.infrastructure.persistence.PatientPreRegistrationEntity(
                orgA.getId(),
                "Spammy",
                "Robot",
                "MASCULIN",
                LocalDate.of(2000, 1, 1),
                null, null, null, null, null, null, null
        );
        entity = preRegistrationRepository.save(entity);
        TenantContext.clear();

        mockMvc.perform(post("/api/pre-registrations/" + entity.getId() + "/reject")
                .header("Authorization", "Bearer " + tokenAgentA))
                .andExpect(status().isNoContent());

        // Assert status is updated to REJECTED
        TenantContext.setTenantId(orgA.getId());
        var updated = preRegistrationRepository.findById(entity.getId()).orElseThrow();
        TenantContext.clear();
        org.junit.jupiter.api.Assertions.assertEquals(PreRegistrationStatus.REJECTED, updated.getStatus());
    }
}
