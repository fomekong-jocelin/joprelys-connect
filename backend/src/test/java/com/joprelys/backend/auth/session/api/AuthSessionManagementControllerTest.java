package com.joprelys.backend.auth.session.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.rbac.RbacStore;
import com.joprelys.backend.auth.session.application.IssueAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionAuditEventRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.RevokedAccessTokenRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionManagementControllerTest {

    private static final SessionClientMetadata METADATA = new SessionClientMetadata(
            "WEB", "Mozilla/5.0 Chrome/150 Windows", "10.20.30.0/24");

    @Autowired MockMvc mockMvc;
    @Autowired IssueAuthSessionUseCase issueUseCase;
    @Autowired AuthSessionRepository sessionRepository;
    @Autowired AuthSessionAuditEventRepository auditRepository;
    @Autowired RevokedAccessTokenRepository revokedTokenRepository;
    @Autowired UserAccountRepository userRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired RbacStore rbacStore;

    private OrganizationEntity organizationA;
    private OrganizationEntity organizationB;
    private UserAccountEntity owner;
    private UserAccountEntity target;
    private UserAccountEntity admin;
    private UserAccountEntity otherTenant;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        revokedTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        rbacStore.seedCatalog();
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        organizationA = organizationRepository.save(new OrganizationEntity(
                "Clinique A " + suffix, "sessions-a-" + suffix + "@joprelys.local", "123", "Rue A", "Douala"));
        organizationB = organizationRepository.save(new OrganizationEntity(
                "Clinique B " + suffix, "sessions-b-" + suffix + "@joprelys.local", "456", "Rue B", "Douala"));
        owner = saveUser("owner-" + suffix + "@joprelys.local", "AGENT_ACCUEIL", organizationA.getId());
        target = saveUser("target-" + suffix + "@joprelys.local", "AGENT_ACCUEIL", organizationA.getId());
        admin = saveUser("admin-" + suffix + "@joprelys.local", "ADMIN_CLINIQUE", organizationA.getId());
        otherTenant = saveUser("other-" + suffix + "@joprelys.local", "AGENT_ACCUEIL", organizationB.getId());
    }

    @Test
    void shouldListAndRevokeOwnCurrentSessionWithoutExposingSecrets() throws Exception {
        IssuedAuthSession session = issueUseCase.issue(owner, METADATA);

        mockMvc.perform(get("/api/auth/sessions").header("Authorization", bearer(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(session.sessionId().toString()))
                .andExpect(jsonPath("$[0].current").value(true))
                .andExpect(jsonPath("$[0].networkHint").value("10.20.30.0/24"))
                .andExpect(jsonPath("$[0].refreshToken").doesNotExist())
                .andExpect(jsonPath("$[0].refreshTokenHash").doesNotExist());

        mockMvc.perform(delete("/api/auth/sessions/{sessionId}", session.sessionId())
                        .header("Authorization", bearer(session)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/api/auth/sessions").header("Authorization", bearer(session)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowSameTenantAdminAndHideCrossTenantSessions() throws Exception {
        IssuedAuthSession adminSession = issueUseCase.issue(admin, METADATA);
        IssuedAuthSession targetSession = issueUseCase.issue(target, METADATA);
        IssuedAuthSession otherSession = issueUseCase.issue(otherTenant, METADATA);

        mockMvc.perform(get("/api/auth/users/{userId}/sessions", target.getId())
                        .header("Authorization", bearer(adminSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(targetSession.sessionId().toString()));

        mockMvc.perform(delete("/api/auth/sessions/{sessionId}", targetSession.sessionId())
                        .header("Authorization", bearer(adminSession)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/sessions").header("Authorization", bearer(targetSession)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/users/{userId}/sessions", otherTenant.getId())
                        .header("Authorization", bearer(adminSession)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("AUTH_SESSION_NOT_FOUND"));

        mockMvc.perform(delete("/api/auth/sessions/{sessionId}", otherSession.sessionId())
                        .header("Authorization", bearer(adminSession)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("AUTH_SESSION_NOT_FOUND"));
    }

    @Test
    void shouldRejectCrossUserActionWithoutPermissionAndRevokeAllOwnSessions() throws Exception {
        IssuedAuthSession ownerFirst = issueUseCase.issue(owner, METADATA);
        IssuedAuthSession ownerSecond = issueUseCase.issue(owner, METADATA);
        IssuedAuthSession targetSession = issueUseCase.issue(target, METADATA);

        mockMvc.perform(delete("/api/auth/sessions/{sessionId}", targetSession.sessionId())
                        .header("Authorization", bearer(ownerFirst)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", bearer(ownerFirst)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(header().string(
                        "Clear-Site-Data",
                        "\"cache\", \"cookies\", \"storage\""));

        mockMvc.perform(get("/api/auth/sessions").header("Authorization", bearer(ownerSecond)))
                .andExpect(status().isUnauthorized());
    }

    private UserAccountEntity saveUser(String email, String role, UUID organizationId) {
        UserAccountEntity user = new UserAccountEntity(email, email, role, "hash");
        user.setOrganizationId(organizationId);
        user = userRepository.save(user);
        rbacStore.synchronizeLegacyAssignments(user);
        return user;
    }

    private static String bearer(IssuedAuthSession session) {
        return "Bearer " + session.accessToken();
    }
}
