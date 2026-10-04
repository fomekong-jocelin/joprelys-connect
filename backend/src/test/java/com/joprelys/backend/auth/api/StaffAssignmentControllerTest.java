package com.joprelys.backend.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.hospitalorganization.domain.OrganizationalUnitType;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitEntity;
import com.joprelys.backend.hospitalorganization.infrastructure.persistence.OrganizationalUnitRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrganizationalUnitRepository organizationalUnitRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserAccountEntity adminA;
    private UserAccountEntity doctorA;
    private UserAccountEntity doctorB;
    private OrganizationalUnitEntity generalMedicineA;
    private OrganizationalUnitEntity emergencyA;
    private OrganizationalUnitEntity generalMedicineB;
    private String adminToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        orgA = organizationRepository.save(new OrganizationEntity(
                "Clinique Staff A " + suffix,
                "staff-assign-a-" + suffix + "@joprelys.local",
                "123",
                "Street A",
                "Douala"));
        orgB = organizationRepository.save(new OrganizationEntity(
                "Clinique Staff B " + suffix,
                "staff-assign-b-" + suffix + "@joprelys.local",
                "456",
                "Street B",
                "Yaounde"));

        adminA = saveUser("admin.staff.a-" + suffix + "@joprelys.local", "Admin Staff A", "ADMIN_CLINIQUE", orgA);
        doctorA = saveUser("doctor.staff.a-" + suffix + "@joprelys.local", "Dr Staff A", "MEDECIN", orgA);
        doctorB = saveUser("doctor.staff.b-" + suffix + "@joprelys.local", "Dr Staff B", "MEDECIN", orgB);

        TenantContext.setTenantId(orgA.getId());
        try {
            generalMedicineA = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    orgA.getId(), null, "MED-GEN-" + suffix, null, OrganizationalUnitType.SERVICE, "GENERAL_MEDICINE"));
            emergencyA = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    orgA.getId(), null, "URG-" + suffix, null, OrganizationalUnitType.SERVICE, "EMERGENCY"));
        } finally {
            TenantContext.clear();
        }

        TenantContext.setTenantId(orgB.getId());
        try {
            generalMedicineB = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    orgB.getId(), null, "MED-GEN-B-" + suffix, null, OrganizationalUnitType.SERVICE, "GENERAL_MEDICINE"));
        } finally {
            TenantContext.clear();
        }

        adminToken = jwtService.createToken(adminA).value();
        doctorToken = jwtService.createToken(doctorA).value();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        cleanupTenant(orgA);
        cleanupTenant(orgB);
    }

    @Test
    void givenAdmin_whenCreateStructuredAssignments_thenHistoryIsReadable() throws Exception {
        Instant from = Instant.now().minusSeconds(60);

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialtyRequest("GENERAL_MEDICINE", true, from, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialtyCode").value("GENERAL_MEDICINE"))
                .andExpect(jsonPath("$.primary").value(true))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineA.getId().toString(), "PRACTITIONER", true, from, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationalUnitId").value(generalMedicineA.getId().toString()))
                .andExpect(jsonPath("$.assignmentRoleCode").value("PRACTITIONER"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/staff/" + doctorA.getId() + "/assignments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(1))
                .andExpect(jsonPath("$.unitAssignments.length()").value(1));
    }

    @Test
    void governanceAssignmentsRequireTheCorrespondingProfessionalQualification() throws Exception {
        Instant from = Instant.now().minusSeconds(60);
        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineA.getId().toString(), "MEDICAL_HEAD", false, from, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentRoleCode").value("MEDICAL_HEAD"));
        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineA.getId().toString(), "NURSE_MANAGER", false, from, null)))
                .andExpect(status().isBadRequest());
        UserAccountEntity nurse = saveUser("nurse.governance-" + UUID.randomUUID() + "@test.local", "Major", "INFIRMIER", orgA);
        mockMvc.perform(post("/api/staff/" + nurse.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineA.getId().toString(), "NURSE_MANAGER", false, from, null)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/staff/" + nurse.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineA.getId().toString(), "MEDICAL_HEAD", false, from, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenOverlappingSameSpecialty_whenCreate_thenRejects() throws Exception {
        Instant from = Instant.now().minusSeconds(60);
        Instant to = Instant.now().plusSeconds(7200);
        createSpecialty(from, to, "GENERAL_MEDICINE", false);

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialtyRequest("GENERAL_MEDICINE", false, Instant.now(), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cette spécialité chevauche une affectation existante."));
    }

    @Test
    void givenSecondPrimaryUnitOnSamePeriod_whenCreate_thenRejects() throws Exception {
        Instant from = Instant.now().minusSeconds(60);
        createUnit(from, null, generalMedicineA.getId().toString(), true);

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(emergencyA.getId().toString(), "PRACTITIONER", true, from, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Une autre unité principale couvre déjà cette période."));
    }

    @Test
    void givenCrossTenantUnit_whenAssign_thenRejects() throws Exception {
        Instant from = Instant.now().minusSeconds(60);

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(generalMedicineB.getId().toString(), "PRACTITIONER", false, from, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unité organisationnelle inactive ou inconnue."));
    }

    @Test
    void givenCrossTenantStaff_whenReadAssignments_thenReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/staff/" + doctorB.getId() + "/assignments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenActiveAssignment_whenClose_thenHistoryIsPreservedAsInactive() throws Exception {
        Instant from = Instant.now().minusSeconds(3600);
        String response = mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialtyRequest("GENERAL_MEDICINE", true, from, null)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String assignmentId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/specialties/" + assignmentId + "/close")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/staff/" + doctorA.getId() + "/assignments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(1))
                .andExpect(jsonPath("$.specialties[0].active").value(false));
    }

    @Test
    void givenDoctorWithoutUserManage_whenCreateAssignment_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(
                                generalMedicineA.getId().toString(),
                                "PRACTITIONER",
                                false,
                                Instant.now().minusSeconds(60),
                                null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAdmin_whenListAssignmentRoles_thenReturnsControlledCatalog() throws Exception {
        mockMvc.perform(get("/api/staff/assignment-roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'PRACTITIONER')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'NURSE')]").exists());
    }

    private void createSpecialty(Instant from, Instant to, String code, boolean primary) throws Exception {
        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialtyRequest(code, primary, from, to)))
                .andExpect(status().isCreated());
    }

    private void createUnit(Instant from, Instant to, String unitId, boolean primary) throws Exception {
        mockMvc.perform(post("/api/staff/" + doctorA.getId() + "/assignments/units")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unitRequest(unitId, "PRACTITIONER", primary, from, to)))
                .andExpect(status().isCreated());
    }

    private String specialtyRequest(String code, boolean primary, Instant from, Instant to) {
        return """
                {
                  "specialtyCode": "%s",
                  "primary": %s,
                  "validFrom": "%s",
                  "validTo": %s
                }
                """.formatted(code, primary, from, to == null ? "null" : "\"" + to + "\"");
    }

    private String unitRequest(String unitId, String role, boolean primary, Instant from, Instant to) {
        return """
                {
                  "organizationalUnitId": "%s",
                  "assignmentRoleCode": "%s",
                  "primary": %s,
                  "validFrom": "%s",
                  "validTo": %s
                }
                """.formatted(unitId, role, primary, from, to == null ? "null" : "\"" + to + "\"");
    }

    private UserAccountEntity saveUser(String email, String displayName, String role, OrganizationEntity organization) {
        UserAccountEntity user = new UserAccountEntity(
                email,
                displayName,
                role,
                passwordEncoder.encode("Staff@12345"));
        user.setOrganizationId(organization.getId());
        return userAccountRepository.save(user);
    }

    private void cleanupTenant(OrganizationEntity organization) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        UUID organizationId = organization.getId();
        jdbcTemplate.update("DELETE FROM staff_organizational_unit_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM staff_specialty_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizational_units WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM users WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
    }
}
