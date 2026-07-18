package com.joprelys.backend.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.session.domain.AuthSessionAuditEventType;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionAuditEventRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.RevokedAccessTokenRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersistentAuthSessionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired AuthSessionRepository sessionRepository;
    @Autowired AuthSessionAuditEventRepository auditRepository;
    @Autowired RevokedAccessTokenRepository revokedTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String loginEmail;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        revokedTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        loginEmail = "agent.sessions-" + suffix + "@joprelys.local";

        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Sessions " + suffix,
                "sessions-" + suffix + "@joprelys.local",
                "+237600000001",
                "Rue des médecins",
                "Douala"));
        UserAccountEntity user = new UserAccountEntity(
                loginEmail,
                "Agent Sessions",
                "AGENT_ACCUEIL",
                passwordEncoder.encode("Password123!"));
        user.setOrganizationId(organization.getId());
        userAccountRepository.save(user);
    }

    @Test
    void shouldRevokeReplacementAccessWhenRotatedRefreshTokenIsReplayed() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Joprelys-Test/1.0")
                        .content("""
                                {
                                  "email": "%s",
	                                  "password": "Password123!"
	                                }
	                                """.formatted(loginEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.sessionExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("joprelys_refresh="),
                        org.hamcrest.Matchers.containsString("HttpOnly"),
                        org.hamcrest.Matchers.containsString("SameSite=Lax"),
                        org.hamcrest.Matchers.containsString("Path=/api/auth"))))
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(login.getResponse().getContentAsString());
        String firstSessionId = loginBody.get("sessionId").asText();
        Cookie firstCookie = login.getResponse().getCookie("joprelys_refresh");
        assertNotNull(firstCookie);

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(firstCookie)
                        .header("User-Agent", "Joprelys-Test/1.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refresh.getResponse().getContentAsString());
        String secondSessionId = refreshBody.get("sessionId").asText();
        String secondAccessToken = refreshBody.get("accessToken").asText();
        assertNotEquals(firstSessionId, secondSessionId);

        List<AuthSessionEntity> family = sessionRepository.findAll();
        assertEquals(2, family.size());
        AuthSessionEntity first = family.stream()
                .filter(session -> session.getId().toString().equals(firstSessionId))
                .findFirst()
                .orElseThrow();
        AuthSessionEntity second = family.stream()
                .filter(session -> session.getId().toString().equals(secondSessionId))
                .findFirst()
                .orElseThrow();
        assertEquals(first.getTokenFamilyId(), second.getTokenFamilyId());
        assertEquals("ROTATED", first.getRevocationReason());
        assertEquals(second.getId(), first.getReplacedBySessionId());

        mockMvc.perform(post("/api/auth/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("AUTH_SESSION_INVALID"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        AuthSessionEntity revokedReplacement = sessionRepository.findById(second.getId()).orElseThrow();
        assertEquals("REPLAY_DETECTED", revokedReplacement.getRevocationReason());
        assertEquals("SYSTEM", revokedReplacement.getRevocationSource());
        assertEquals(true, auditRepository.existsByEventTypeAndTokenFamilyId(
                AuthSessionAuditEventType.REFRESH_REPLAY_DETECTED.name(), first.getTokenFamilyId()));

        mockMvc.perform(get("/api/auth/sessions")
                        .header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldClearCookieAndBrowserSiteDataOnLogout() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
	                                  "password": "Password123!"
	                                }
	                                """.formatted(loginEmail)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(login.getResponse().getContentAsString());
        Cookie refreshCookie = login.getResponse().getCookie("joprelys_refresh");
        assertNotNull(refreshCookie);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + loginBody.get("accessToken").asText())
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        "Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")))
                .andExpect(header().string(
                        "Clear-Site-Data",
                        "\"cache\", \"cookies\", \"storage\""));
    }
}
