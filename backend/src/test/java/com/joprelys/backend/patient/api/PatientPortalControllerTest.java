package com.joprelys.backend.patient.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentEntity;
import com.joprelys.backend.visit.infrastructure.persistence.MedicalDocumentRepository;
import com.joprelys.backend.visit.infrastructure.persistence.VisitEntity;
import com.joprelys.backend.visit.infrastructure.persistence.VisitRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PatientPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private MedicalDocumentRepository medicalDocumentRepository;

    @Autowired
    private com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository userAccountRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    private OrganizationEntity orgA;
    private PatientEntity patientA;
    private PatientEntity patientB;
    private VisitEntity visitA;
    private String tokenPatientA;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // 1. Créer Organisation
        orgA = new OrganizationEntity("Clinique Test A", "contact@testa.org", "123456", "Adresse A", "Douala");
        orgA = organizationRepository.save(orgA);

        TenantContext.setTenantId(orgA.getId());

        // 2. Créer Patients
        patientA = new PatientEntity(
                "PAT-20260702-000001",
                "LOC-A-001",
                "Jean Patient A",
                "MASCULIN",
                LocalDate.of(1990, 1, 1),
                "+237 699 99 99 99",
                "Douala",
                "Akwa",
                "Rue 1",
                "Marie",
                "+237 677 77 77 77",
                "Aucune",
                "Aucun"
        );
        patientA = patientRepository.save(patientA);

        patientB = new PatientEntity(
                "PAT-20260702-000002",
                "LOC-A-002",
                "Paul Patient B",
                "MASCULIN",
                LocalDate.of(1995, 5, 5),
                "+237 688 88 88 88",
                "Yaoundé",
                "Messa",
                "Rue 2",
                "Pierre",
                "+237 666 66 66 66",
                "Pénicilline",
                "Hypertension"
        );
        patientB = patientRepository.save(patientB);

        // 3. Créer une visite clôturée pour le Patient A
        visitA = new VisitEntity(patientA, "VIS-20260702-000001", "Consultation générale", "PÉDIATRIE");
        visitA.setOrganizationId(orgA.getId());
        visitA.setStatus("CLOTUREE");
        visitA = visitRepository.save(visitA);

        // 4. Créer un document médical pour le Patient A
        MedicalDocumentEntity docA = new MedicalDocumentEntity(
                visitA,
                "DOC-20260702-000001",
                "target/test-classes/test.pdf"
        );
        docA.setOrganizationId(orgA.getId());
        medicalDocumentRepository.save(docA);

        // 5. Générer le Token JWT pour Patient A
        JwtService.CreatedToken createdToken = jwtService.createPatientToken(patientA);
        tokenPatientA = createdToken.value();

        TenantContext.clear();
    }

    @Test
    void givenValidPatient_whenRequestOtp_thenOk() throws Exception {
        String json = """
                {
                    "globalPatientNumber": "%s",
                    "phone": "%s",
                    "birthDate": "%s"
                }
                """.formatted(patientA.getGlobalPatientNumber(), patientA.getPhone(), patientA.getBirthDate().toString());

        mockMvc.perform(post("/api/public/patient/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void givenInvalidPatientPhone_whenRequestOtp_thenNotFound() throws Exception {
        String json = """
                {
                    "globalPatientNumber": "%s",
                    "phone": "+33600000000",
                    "birthDate": "%s"
                }
                """.formatted(patientA.getGlobalPatientNumber(), patientA.getBirthDate().toString());

        mockMvc.perform(post("/api/public/patient/auth/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenPatientMe_whenAuthorized_thenReturnProfile() throws Exception {
        mockMvc.perform(get("/api/patient/me")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Jean Patient A"))
                .andExpect(jsonPath("$.globalPatientNumber").value(patientA.getGlobalPatientNumber()))
                .andExpect(jsonPath("$.allergies").value("Aucune"));
    }

    @Test
    void givenPatientMe_whenNoToken_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/patient/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenPatientOwnDocument_whenDownload_thenOk() throws Exception {
        java.io.File tempFile = new java.io.File("target/test-classes/test.pdf");
        if (!tempFile.exists()) {
            tempFile.getParentFile().mkdirs();
            java.nio.file.Files.write(tempFile.toPath(), new byte[]{1, 2, 3});
        }

        mockMvc.perform(get("/api/patient/visits/" + visitA.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isOk());
    }

    @Test
    void givenPatientOtherDocument_whenDownload_thenForbidden() throws Exception {
        // Créer une visite et un document pour le Patient B
        TenantContext.setTenantId(orgA.getId());
        VisitEntity visitB = new VisitEntity(patientB, "VIS-20260702-000002", "Consultation cardiologie", "CARDIOLOGIE");
        visitB.setOrganizationId(orgA.getId());
        visitB.setStatus("CLOTUREE");
        visitB = visitRepository.save(visitB);

        MedicalDocumentEntity docB = new MedicalDocumentEntity(
                visitB,
                "DOC-20260702-000002",
                "target/test-classes/test2.pdf"
        );
        docB.setOrganizationId(orgA.getId());
        medicalDocumentRepository.save(docB);
        TenantContext.clear();

        // Le Patient A tente de télécharger le document du Patient B
        mockMvc.perform(get("/api/patient/visits/" + visitB.getId() + "/document")
                .header("Authorization", "Bearer " + tokenPatientA))
                .andExpect(status().isForbidden());
    }
}
