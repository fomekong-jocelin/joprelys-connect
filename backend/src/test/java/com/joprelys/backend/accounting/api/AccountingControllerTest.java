package com.joprelys.backend.accounting.api;

import com.joprelys.backend.accounting.application.AccountingExportService;
import com.joprelys.backend.auth.infrastructure.persistence.*;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AccountingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private OrganizationEntity org;
    private String tokenDaf;
    private String tokenCaissier;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public AccountingExportService mockAccountingExportService() {
            AccountingExportService mock = Mockito.mock(AccountingExportService.class);
            Mockito.when(mock.generateSage100Export(any(), any()))
                   .thenReturn("Journal;Date;CompteGeneral;CompteTiers;RefPiece;Libelle;Debit;Credit\nCA;090726;57110000;;REC-01;Reçu;1000;0");
            return mock;
        }
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payment_receipts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        userAccountRepository.deleteAll();
        organizationRepository.deleteAll();

        // Org
        org = new OrganizationEntity("Clinique Test", "test@test.local", "999999", "Avenue Test", "Yaounde");
        org = organizationRepository.save(org);
        TenantContext.setTenantId(org.getId());

        // Créer un utilisateur DAF
        UserAccountEntity daf = new UserAccountEntity("daf@test.com", "Pierre DAF", "DAF", "password");
        daf.setOrganizationId(org.getId());
        daf = userAccountRepository.save(daf);
        tokenDaf = jwtService.createToken(daf).value();

        // Créer un caissier
        UserAccountEntity caissier = new UserAccountEntity("caissier@test.com", "Jean Caissier", "CAISSIER", "password");
        caissier.setOrganizationId(org.getId());
        caissier = userAccountRepository.save(caissier);
        tokenCaissier = jwtService.createToken(caissier).value();
    }

    @Test
    void shouldExportSageCsvForDaf() throws Exception {
        TenantContext.setTenantId(org.getId());
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenDaf)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("export_sage_100")))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("REC-01")));
    }

    @Test
    void shouldDenyExportForCaissier() throws Exception {
        TenantContext.setTenantId(org.getId());
        mockMvc.perform(get("/api/accounting/export")
                        .header("Authorization", "Bearer " + tokenCaissier)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
