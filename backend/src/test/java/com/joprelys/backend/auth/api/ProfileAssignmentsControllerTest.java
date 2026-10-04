package com.joprelys.backend.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffOrganizationalUnitAssignmentRepository;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentEntity;
import com.joprelys.backend.auth.infrastructure.persistence.StaffSpecialtyAssignmentRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileAssignmentsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired private StaffSpecialtyAssignmentRepository specialtyAssignmentRepository;
    @Autowired private StaffOrganizationalUnitAssignmentRepository unitAssignmentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;
    private UserAccountEntity doctor;
    private String token;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Profil " + suffix,
                "profile-" + suffix + "@joprelys.local",
                "123",
                "Street",
                "Douala"));
        doctor = new UserAccountEntity(
                "doctor.profile-" + suffix + "@joprelys.local",
                "Dr Profil",
                "MEDECIN",
                passwordEncoder.encode("Staff@12345"));
        doctor.setOrganizationId(organization.getId());
        doctor.setRegistrationNumber("ONMC-PROFILE-001");
        doctor = userAccountRepository.save(doctor);

        TenantContext.setTenantId(organization.getId());
        try {
            OrganizationalUnitEntity unit = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    organization.getId(),
                    null,
                    "MED-PROFILE-" + suffix,
                    null,
                    OrganizationalUnitType.SERVICE,
                    "GENERAL_MEDICINE"));
            Instant from = Instant.now().minusSeconds(60);
            specialtyAssignmentRepository.save(new StaffSpecialtyAssignmentEntity(
                    organization.getId(), doctor.getId(), "GENERAL_MEDICINE", true, from, null));
            unitAssignmentRepository.save(new StaffOrganizationalUnitAssignmentEntity(
                    organization.getId(), doctor.getId(), unit.getId(), "PRACTITIONER", true, from, null));
        } finally {
            TenantContext.clear();
        }

        token = jwtService.createToken(doctor).value();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        UUID organizationId = organization.getId();
        jdbcTemplate.update("DELETE FROM staff_organizational_unit_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM staff_specialty_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizational_units WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM users WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
    }

    @Test
    void givenStaffWithActiveAssignments_whenReadOwnProfileAssignments_thenReturnsResolvedBusinessLabels() throws Exception {
        mockMvc.perform(get("/api/profile/assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialties.length()").value(1))
                .andExpect(jsonPath("$.specialties[0].specialtyCode").value("GENERAL_MEDICINE"))
                .andExpect(jsonPath("$.specialties[0].nameFr").value("Médecine générale"))
                .andExpect(jsonPath("$.specialties[0].primary").value(true))
                .andExpect(jsonPath("$.unitAssignments.length()").value(1))
                .andExpect(jsonPath("$.unitAssignments[0].nameFr").value("Médecine générale"))
                .andExpect(jsonPath("$.unitAssignments[0].assignmentRoleCode").value("PRACTITIONER"))
                .andExpect(jsonPath("$.unitAssignments[0].assignmentRoleNameFr").value("Praticien"))
                .andExpect(jsonPath("$.unitAssignments[0].primary").value(true));
    }
    @Test
    void platformAdministratorWithoutOrganizationHasAnEmptyProfessionalContext() throws Exception {
        UserAccountEntity admin = userAccountRepository.save(new UserAccountEntity(
                "platform-profile-" + UUID.randomUUID() + "@joprelys.local", "Admin plateforme", "ADMIN_JOPRELYS", "hash"));
        try {
            String adminToken = jwtService.createToken(admin).value();
            mockMvc.perform(get("/api/profile/assignments").header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.specialties.length()").value(0))
                    .andExpect(jsonPath("$.unitAssignments.length()").value(0));
        } finally {
            jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", admin.getId());
            userAccountRepository.deleteById(admin.getId());
        }
    }

}
