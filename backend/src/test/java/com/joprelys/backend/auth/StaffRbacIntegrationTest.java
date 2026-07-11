package com.joprelys.backend.auth;

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
import java.util.List;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaffRbacIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrganizationEntity organization;
    private UserAccountEntity admin;
    private String adminToken;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique équipe " + suffix,
                "team-" + suffix + "@test.local",
                "600000003",
                "Centre",
                "Douala"));
        TenantContext.setTenantId(organization.getId());
        admin = new UserAccountEntity(
                "admin-team-" + suffix + "@test.local",
                "Admin équipe",
                "ADMIN_CLINIQUE",
                "password");
        admin.setOrganizationId(organization.getId());
        admin = userAccountRepository.saveAndFlush(admin);
        adminToken = jwtService.createToken(admin).value();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateTeamMemberWithSystemAndCustomRolesAndApplyPermissionsImmediately() throws Exception {
        mockMvc.perform(post("/api/rbac/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "SUPERVISEUR_CAISSE_EQUIPE",
                                "name", "Superviseur caisse équipe",
                                "assignable", true,
                                "permissionCodes", List.of("CASH_HISTORY_READ")))))
                .andExpect(status().isCreated());

        String email = "finance-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        mockMvc.perform(post("/api/staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "displayName", "Responsable financier",
                                "roles", List.of("DAF", "CAISSIER", "SUPERVISEUR_CAISSE_EQUIPE")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DAF,CAISSIER,SUPERVISEUR_CAISSE_EQUIPE"));

        TenantContext.setTenantId(organization.getId());
        UserAccountEntity created = userAccountRepository.findByEmail(email).orElseThrow();
        String createdToken = jwtService.createToken(created).value();
        TenantContext.clear();

        mockMvc.perform(get("/api/rbac/me")
                        .header("Authorization", "Bearer " + createdToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@ == 'DAF')]").exists())
                .andExpect(jsonPath("$.roles[?(@ == 'CAISSIER')]").exists())
                .andExpect(jsonPath("$.roles[?(@ == 'SUPERVISEUR_CAISSE_EQUIPE')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'CASH_HISTORY_READ')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'CASH_PAYMENT_COLLECT')]").exists());
    }
}
