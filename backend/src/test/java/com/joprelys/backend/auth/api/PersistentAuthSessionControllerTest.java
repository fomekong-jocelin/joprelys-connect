package com.joprelys.backend.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionEntity;
import com.joprelys.backend.auth.session.infrastructure.persistence.AuthSessionRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
    @Autowired ObjectMapper objectMapper;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired AuthSessionRepository sessionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        OrganizationEntity organization = organizationRepository.save(new OrganizationEntity(
                "Clinique Sessions",
                "sessions@joprelys.local",
                "+237600000001",
                "Rue des médecins",
                "Douala"));
        UserAccountEntity user = new UserAccountEntity(
                "agent.sessions@joprelys.local",
                "Agent Sessions",
                "AGENT_ACCUEIL",
                passwordEncoder.encode("Password123!"));
        user.setOrganizationId(organization.getId());
        userAccountRepository.save(user);
    }

    @Test
    void shouldIssueHttpOnlyCookieRotateOnceAndRejectConsumedToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Joprelys-Test/1.0")
                        .content("""
                                {
                                  "email": "agent.sessions@joprelys.local",
                                  "password": "Password123!"
                                }
                                """))
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
    }
}
