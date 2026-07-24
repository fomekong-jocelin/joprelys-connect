package com.joprelys.backend.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.joprelys.backend.notification.application.AccountMailService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffOnboardingControllerTest {

    @MockitoBean
    private AccountMailService accountMailService;

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
    private OrganizationalUnitEntity medicineA;
    private OrganizationalUnitEntity medicineB;
    private String adminToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        orgA = organizationRepository.save(new OrganizationEntity(
                "Clinique Onboarding A " + suffix,
                "onboarding-a-" + suffix + "@joprelys.local",
                "123",
                "Street A",
                "Douala"));
        orgB = organizationRepository.save(new OrganizationEntity(
                "Clinique Onboarding B " + suffix,
                "onboarding-b-" + suffix + "@joprelys.local",
                "456",
                "Street B",
                "Yaounde"));

        adminA = saveUser(
                "admin.onboarding-" + suffix + "@joprelys.local",
                "Admin Onboarding",
                "ADMIN_CLINIQUE",
                orgA);

        TenantContext.setTenantId(orgA.getId());
        try {
            medicineA = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    orgA.getId(),
                    null,
                    "MED-ONB-" + suffix,
                    null,
                    OrganizationalUnitType.SERVICE,
                    "GENERAL_MEDICINE"));
        } finally {
            TenantContext.clear();
        }

        TenantContext.setTenantId(orgB.getId());
        try {
            medicineB = organizationalUnitRepository.save(new OrganizationalUnitEntity(
                    orgB.getId(),
                    null,
                    "MED-ONB-B-" + suffix,
                    null,
                    OrganizationalUnitType.SERVICE,
                    "GENERAL_MEDICINE"));
        } finally {
            TenantContext.clear();
        }

        adminToken = jwtService.createToken(adminA).value();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        cleanupTenant(orgA);
        cleanupTenant(orgB);
    }

    @Test
    void givenCompleteDoctorOnboarding_whenInvite_thenProfileAndAssignmentsAreCreatedTogether() throws Exception {
        String email = "dr.onboarding." + UUID.randomUUID().toString().substring(0, 8) + "@joprelys.local";
        Instant from = Instant.now().minusSeconds(30);
        String request = """
                {
                  "email": "%s",
                  "displayName": "Dr Démo",
                  "roles": ["MEDECIN"],
                  "phone": "+237677889900",
                  "registrationNumber": "ONMC-DEMO-001",
                  "bio": "Médecin de démonstration",
                  "specialtyAssignments": [{
                    "specialtyCode": "GENERAL_MEDICINE",
                    "primary": true,
                    "validFrom": "%s"
                  }],
                  "unitAssignments": [{
                    "organizationalUnitId": "%s",
                    "assignmentRoleCode": "PRACTITIONER",
                    "primary": true,
                    "validFrom": "%s"
                  }]
                }
                """.formatted(email, from, medicineA.getId(), from);

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MEDECIN"));

        UserAccountEntity created = userAccountRepository.findByEmail(email).orElseThrow();
        assertThat(created.getOrganizationId()).isEqualTo(orgA.getId());
        assertThat(created.getPhone()).isEqualTo("+237677889900");
        assertThat(created.getRegistrationNumber()).isEqualTo("ONMC-DEMO-001");
        assertThat(created.getBio()).isEqualTo("Médecin de démonstration");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_specialty_assignments WHERE organization_id = ? AND staff_id = ? AND specialty_code = 'GENERAL_MEDICINE'",
                Integer.class,
                orgA.getId(),
                created.getId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_organizational_unit_assignments WHERE organization_id = ? AND staff_id = ? AND organizational_unit_id = ? AND assignment_role_code = 'PRACTITIONER'",
                Integer.class,
                orgA.getId(),
                created.getId(),
                medicineA.getId())).isEqualTo(1);
    }

    @Test
    void givenCrossTenantInitialUnit_whenInvite_thenNothingIsPersisted() throws Exception {
        String email = "dr.invalid." + UUID.randomUUID().toString().substring(0, 8) + "@joprelys.local";
        Instant from = Instant.now().minusSeconds(30);
        String request = """
                {
                  "email": "%s",
                  "displayName": "Dr Invalid",
                  "roles": ["MEDECIN"],
                  "registrationNumber": "ONMC-DEMO-002",
                  "unitAssignments": [{
                    "organizationalUnitId": "%s",
                    "assignmentRoleCode": "PRACTITIONER",
                    "primary": true,
                    "validFrom": "%s"
                  }]
                }
                """.formatted(email, medicineB.getId(), from);

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unité organisationnelle inactive ou inconnue."));

        assertThat(userAccountRepository.findByEmail(email)).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_organizational_unit_assignments WHERE organization_id = ?",
                Integer.class,
                orgA.getId())).isZero();
    }

    @Test
    void givenDoctorWithoutRegistrationNumber_whenInvite_thenRejectsBeforeCreation() throws Exception {
        String email = "dr.no-license." + UUID.randomUUID().toString().substring(0, 8) + "@joprelys.local";
        String request = """
                {
                  "email": "%s",
                  "displayName": "Dr Sans Ordre",
                  "roles": ["MEDECIN"]
                }
                """.formatted(email);

        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le numéro d'inscription à l'Ordre est obligatoire pour un médecin."));

        assertThat(userAccountRepository.findByEmail(email)).isEmpty();
    }

    private UserAccountEntity saveUser(
            String email,
            String displayName,
            String role,
            OrganizationEntity organization) {
        UserAccountEntity user = new UserAccountEntity(
                email,
                displayName,
                role,
                passwordEncoder.encode("Admin@12345"));
        user.setOrganizationId(organization.getId());
        return userAccountRepository.save(user);
    }

    private void cleanupTenant(OrganizationEntity organization) {
        if (organization == null || organization.getId() == null) return;
        UUID organizationId = organization.getId();
        jdbcTemplate.update("DELETE FROM staff_organizational_unit_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM staff_specialty_assignments WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM rbac_audit_log WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM user_roles WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizational_units WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM users WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizations WHERE id = ?", organizationId);
    }
}
