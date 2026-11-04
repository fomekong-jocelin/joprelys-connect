package com.joprelys.backend.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;
import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import com.joprelys.backend.auth.security.JwtService;
import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import com.joprelys.backend.patient.infrastructure.persistence.PatientEntity;
import com.joprelys.backend.patient.infrastructure.persistence.PatientRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * SEC-04 : une écriture d'audit déclenchée depuis une lecture ({@code readOnly = true})
 * doit être réellement persistée, y compris si la transaction de lecture échoue ensuite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogReadOnlyTransactionPersistenceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AuditService auditService;
    @Autowired private PlatformTransactionManager transactionManager;

    private PatientEntity patient;
    private UserAccountEntity agent;
    private String agentToken;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OrganizationEntity organization = organizationRepository.saveAndFlush(new OrganizationEntity(
                "Clinique Audit " + suffix, "audit-" + suffix + "@test.local", "600000010", "Centre", "Douala"));

        agent = new UserAccountEntity("agent-audit-" + suffix + "@test.local", "Agent Audit", "AGENT_ACCUEIL", "hash");
        agent.setOrganizationId(organization.getId());
        agent = userAccountRepository.saveAndFlush(agent);
        agentToken = jwtService.createToken(agent).value();

        TenantContext.setTenantId(organization.getId());
        patient = patientRepository.saveAndFlush(new PatientEntity(
                "DPU-AUD-" + suffix, "PAT-AUD-" + suffix, "Patient Audit", "MASCULIN",
                LocalDate.of(1980, 1, 1), "+237690000010", "Douala", "", "", "", "", "", ""));
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void patientRecordReadIsAuditedEvenThoughTheReadTransactionIsReadOnly() throws Exception {
        mockMvc.perform(get("/api/patients/" + patient.getId())
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        assertThat(countAuditRows(patient.getId(), "CONSULTATION")).isEqualTo(1);
    }

    @Test
    void auditWrittenInsideAReadOnlyTransactionSurvivesItsRollback() {
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        readOnly.executeWithoutResult(transaction -> {
            auditService.logDenied(agent.getId(), agent.getOrganizationId(), patient.getId(),
                    "PATIENT_RECORD", patient.getId(), "READ_DENIED", "Accès refusé de test");
            transaction.setRollbackOnly();
        });

        assertThat(countAuditRows(patient.getId(), "READ_DENIED")).isEqualTo(1);
    }

    private int countAuditRows(UUID patientId, String action) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE patient_id = ? AND action = ?",
                Integer.class, patientId, action);
        return count == null ? 0 : count;
    }
}
