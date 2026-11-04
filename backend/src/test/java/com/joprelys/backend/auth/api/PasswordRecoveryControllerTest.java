package com.joprelys.backend.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.application.PasswordRecoveryService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.OtpChallengeStore;
import com.joprelys.backend.auth.session.application.IssueAuthSessionUseCase;
import com.joprelys.backend.auth.session.application.IssuedAuthSession;
import com.joprelys.backend.auth.session.application.SessionClientMetadata;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.notification.application.AccountMailService;
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

import java.lang.reflect.Field;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PasswordRecoveryControllerTest {

    @MockitoBean
    private AccountMailService accountMailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IssueAuthSessionUseCase issueAuthSessionUseCase;

    private OrganizationEntity orgActive;
    private OrganizationEntity orgInactive;
    private UserAccountEntity userActive;
    private UserAccountEntity userInactiveOrg;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        jdbcTemplate.update("DELETE FROM auth_audit_events");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        orgActive = new OrganizationEntity("Clinique Active", "active@clinique.local", "+2371", "Rue Active", "Douala");
        orgActive = organizationRepository.save(orgActive);

        orgInactive = new OrganizationEntity("Clinique Inactive", "inactive@clinique.local", "+2372", "Rue Inactive", "Douala");
        orgInactive.setStatus("INACTIVE");
        orgInactive = organizationRepository.save(orgInactive);

        userActive = new UserAccountEntity("medecin@joprelys.local", "Dr Active", "MEDECIN", passwordEncoder.encode("motdepasse123"));
        userActive.setOrganizationId(orgActive.getId());
        userActive = userAccountRepository.save(userActive);

        userInactiveOrg = new UserAccountEntity("medecin.inactive@joprelys.local", "Dr Inactive Org", "MEDECIN", passwordEncoder.encode("motdepasse123"));
        userInactiveOrg.setOrganizationId(orgInactive.getId());
        userInactiveOrg = userAccountRepository.save(userInactiveOrg);
    }

    @Test
    void givenActiveUser_whenRequestRecovery_thenSuccess() throws Exception {
        String request = """
                {
                    "email": "medecin@joprelys.local"
                }
                """;

        mockMvc.perform(post("/api/public/auth/password-recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("\"otpCode\""));
                });

        String otp = getOtpFromService("medecin@joprelys.local");
        assertTrue(otp != null && otp.length() == 6);
    }

    @Test
    void givenInactiveOrganization_whenRequestRecovery_thenSuccessButNoOtpGenerated() throws Exception {
        String request = """
                {
                    "email": "medecin.inactive@joprelys.local"
                }
                """;

        mockMvc.perform(post("/api/public/auth/password-recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk()) // Doit renvoyer 200 par sécurité
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("\"otpCode\""));
                });

        String otp = getOtpFromService("medecin.inactive@joprelys.local");
        assertTrue(otp == null);
    }

    @Test
    void givenUnknownEmail_whenRequestRecovery_thenSuccessButNoOtpGenerated() throws Exception {
        String request = """
                {
                    "email": "inconnu@joprelys.local"
                }
                """;

        mockMvc.perform(post("/api/public/auth/password-recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk()) // Doit renvoyer 200 par sécurité
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("\"otpCode\""));
                });

        String otp = getOtpFromService("inconnu@joprelys.local");
        assertTrue(otp == null);
    }

    @Test
    void givenValidOtp_whenResetPassword_thenPasswordUpdated() throws Exception {
        // 1. Demande d'OTP
        String otp = passwordRecoveryService.generateAndSendOtp("medecin@joprelys.local");
        assertTrue(otp != null && otp.length() == 6);

        // 2. Réinitialisation
        String resetRequest = String.format("""
                {
                    "email": "medecin@joprelys.local",
                    "otpCode": "%s",
                    "newPassword": "nouveauMotDePasse789"
                }
                """, otp);

        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest))
                .andExpect(status().isOk());

        // 3. Vérification en base
        UserAccountEntity updatedUser = userAccountRepository.findById(userActive.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("nouveauMotDePasse789", updatedUser.getPasswordHash()));
        assertFalse(passwordEncoder.matches("motdepasse123", updatedUser.getPasswordHash()));
    }

    @Test
    void givenActiveSessions_whenResetPassword_thenAllSessionsAreRevokedAndResetIsAudited() throws Exception {
        jdbcTemplate.update("DELETE FROM audit_logs WHERE action = 'PASSWORD_RESET'");
        IssuedAuthSession session = issueAuthSessionUseCase.issue(
                userActive, new SessionClientMetadata("WEB", "JUnit", "127.0.0.0/24"));
        mockMvc.perform(get("/api/rbac/me").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isOk());

        String otp = passwordRecoveryService.generateAndSendOtp("medecin@joprelys.local");
        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "medecin@joprelys.local", "otpCode": "%s", "newPassword": "nouveauMotDePasse789"}
                                """.formatted(otp)))
                .andExpect(status().isOk());

        assertEquals("PASSWORD_RESET", jdbcTemplate.queryForObject(
                "SELECT revocation_reason FROM auth_sessions WHERE id = ?", String.class, session.sessionId()));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'PASSWORD_RESET' AND resource_id = ?",
                Integer.class, userActive.getId()));
        mockMvc.perform(get("/api/rbac/me").header("Authorization", "Bearer " + session.accessToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void givenInvalidOtp_whenResetPassword_thenBadRequest() throws Exception {
        passwordRecoveryService.generateAndSendOtp("medecin@joprelys.local");

        String resetRequest = """
                {
                    "email": "medecin@joprelys.local",
                    "otpCode": "000000",
                    "newPassword": "nouveauMotDePasse789"
                }
                """;

        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenTooManyAttempts_whenResetPassword_thenOtpDeleted() throws Exception {
        passwordRecoveryService.generateAndSendOtp("medecin@joprelys.local");

        String resetRequest = """
                {
                    "email": "medecin@joprelys.local",
                    "otpCode": "000000",
                    "newPassword": "nouveauMotDePasse789"
                }
                """;

        // 3 tentatives échouées
        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequest));

        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetRequest));

        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest))
                .andExpect(status().isBadRequest());

        // L'OTP doit être détruit
        String otp = getOtpFromService("medecin@joprelys.local");
        assertTrue(otp == null);
    }

    @Test
    void givenShortNewPassword_whenResetPassword_thenValidationFails() throws Exception {
        String resetRequest = """
                {
                    "email": "medecin@joprelys.local",
                    "otpCode": "123456",
                    "newPassword": "short"
                }
                """;

        mockMvc.perform(post("/api/public/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequest))
                .andExpect(status().isBadRequest());
    }

    private String getOtpFromService(String email) {
        try {
            Object target = org.springframework.test.util.AopTestUtils.getTargetObject(passwordRecoveryService);
            Field field = PasswordRecoveryService.class.getDeclaredField("recoveryChallenges");
            field.setAccessible(true);
            OtpChallengeStore store = (OtpChallengeStore) field.get(target);
            return store.activeCodeForTesting(email).orElse(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
