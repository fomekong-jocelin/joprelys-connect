package com.joprelys.backend.auth.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.application.PasswordRecoveryService;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
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

import java.lang.reflect.Field;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PasswordRecoveryControllerTest {

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
                .andExpect(status().isOk());

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
                .andExpect(status().isOk()); // Doit renvoyer 200 par sécurité

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
                .andExpect(status().isOk()); // Doit renvoyer 200 par sécurité

        String otp = getOtpFromService("inconnu@joprelys.local");
        assertTrue(otp == null);
    }

    @Test
    void givenValidOtp_whenResetPassword_thenPasswordUpdated() throws Exception {
        // 1. Demande d'OTP
        passwordRecoveryService.generateAndSendOtp("medecin@joprelys.local");
        String otp = getOtpFromService("medecin@joprelys.local");

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

    @SuppressWarnings("unchecked")
    private String getOtpFromService(String email) {
        try {
            Object target = org.springframework.test.util.AopTestUtils.getTargetObject(passwordRecoveryService);
            Field field = PasswordRecoveryService.class.getDeclaredField("otpMap");
            field.setAccessible(true);
            Map<String, ?> map = (Map<String, ?>) field.get(target);
            if (map == null) return null;
            Object otpData = map.get(email);
            if (otpData == null) return null;
            Field codeField = otpData.getClass().getDeclaredField("code");
            codeField.setAccessible(true);
            return (String) codeField.get(otpData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
