package com.joprelys.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.nio.charset.StandardCharsets;
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
class RbacControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private OrganizationEntity organization;
    private OrganizationEntity otherOrganization;
    private UserAccountEntity admin;
    private UserAccountEntity staff;
    private UserAccountEntity outsider;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique RBAC " + suffix, "rbac-" + suffix + "@test.local", "600000001", "Centre", "Douala"));
        otherOrganization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique externe " + suffix, "other-" + suffix + "@test.local", "600000002", "Centre", "Yaoundé"));

        TenantContext.setTenantId(organization.getId());
        admin = saveUser("admin-" + suffix + "@test.local", "Admin RBAC", "ADMIN_CLINIQUE", organization.getId());
        staff = saveUser("staff-" + suffix + "@test.local", "Collaborateur RBAC", "AGENT_ACCUEIL", organization.getId());
        adminToken = jwtService.createToken(admin).value();
        staffToken = jwtService.createToken(staff).value();

        TenantContext.setTenantId(otherOrganization.getId());
        outsider = saveUser("outsider-" + suffix + "@test.local", "Utilisateur externe", "AGENT_ACCUEIL", otherOrganization.getId());
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeMissingSystemRolesAndPermissionsToAdmin() throws Exception {
        mockMvc.perform(get("/api/rbac/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'DAF')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CAISSIER')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'SECRETAIRE_COMPTABLE')]").exists());

        mockMvc.perform(get("/api/rbac/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'RBAC_MANAGE')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CASH_PAYMENT_COLLECT')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'INSURANCE_BORDEREAU_SETTLE')]").exists());
    }

    @Test
    void shouldCreateCustomRoleAssignItAndResolvePermissionsImmediately() throws Exception {
        String createResponse = mockMvc.perform(post("/api/rbac/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "CAISSE_SUPERVISEUR",
                                "name", "Superviseur caisse",
                                "description", "Contrôle les opérations de caisse.",
                                "assignable", true,
                                "permissionCodes", List.of("CASH_HISTORY_READ", "ACCOUNTING_DASHBOARD_READ")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.systemRole").value(false))
                .andExpect(jsonPath("$.permissions[?(@ == 'CASH_HISTORY_READ')]").exists())
                .andReturn().getResponse().getContentAsString();

        UUID roleId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());
        mockMvc.perform(put("/api/rbac/users/" + staff.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("roleIds", List.of(roleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@ == 'CAISSE_SUPERVISEUR')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'CASH_HISTORY_READ')]").exists());

        mockMvc.perform(get("/api/rbac/me")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[?(@ == 'CAISSE_SUPERVISEUR')]").exists())
                .andExpect(jsonPath("$.permissions[?(@ == 'CASH_HISTORY_READ')]").exists());
    }

    @Test
    void shouldRejectSelfPrivilegeChangeAndCrossTenantAssignment() throws Exception {
        mockMvc.perform(put("/api/rbac/users/" + admin.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(roleId("ADMIN_CLINIQUE"))))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/rbac/users/" + outsider.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(roleId("AGENT_ACCUEIL"))))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectPlatformRoleAssignmentByClinicAdmin() throws Exception {
        mockMvc.perform(put("/api/rbac/users/" + staff.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(roleId("SUPER_ADMIN"))))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/rbac/users/" + staff.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roleIds", List.of(roleId("ADMIN_JOPRELYS"))))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDenyRbacAdministrationWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/rbac/roles")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
    }

    private UserAccountEntity saveUser(String email, String name, String role, UUID organizationId) {
        UserAccountEntity user = new UserAccountEntity(email, name, role, "password");
        user.setOrganizationId(organizationId);
        return userAccountRepository.saveAndFlush(user);
    }

    private static UUID roleId(String code) {
        return UUID.nameUUIDFromBytes(("joprelys-role:" + code).getBytes(StandardCharsets.UTF_8));
    }
}
