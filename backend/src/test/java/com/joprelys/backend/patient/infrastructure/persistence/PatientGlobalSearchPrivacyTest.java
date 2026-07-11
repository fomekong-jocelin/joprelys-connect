package com.joprelys.backend.patient.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.security.TenantContext;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationEntity;
import com.joprelys.backend.clinic.infrastructure.persistence.OrganizationRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PatientGlobalSearchPrivacyTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OrganizationEntity organization;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM emergency_admission_requests");
        jdbcTemplate.update("DELETE FROM resuscitation_logs");
        jdbcTemplate.update("DELETE FROM emergencies");
        jdbcTemplate.update("DELETE FROM patient_identity_declarations");
        jdbcTemplate.update("DELETE FROM patient_identity_status_history");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM medical_documents");
        jdbcTemplate.update("DELETE FROM prescription_items");
        jdbcTemplate.update("DELETE FROM prescriptions");
        jdbcTemplate.update("DELETE FROM consultations");
        jdbcTemplate.update("DELETE FROM visits");
        jdbcTemplate.update("DELETE FROM patients");
        organizationRepository.deleteAll();
        organization = organizationRepository.save(
                new OrganizationEntity("Clinique A", "contact@joprelys.local", "123", "Street A", "Douala"));
        TenantContext.setTenantId(organization.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeOnlyVerifiedIdentitiesToGlobalSearch() {
        PatientEntity declared = patientRepository.save(new PatientEntity(
                "DPU-DECLARED", "PAT-DECLARED", "Nom déclaré", "MASCULIN",
                LocalDate.of(1990, 1, 1), "+237600000001", "Douala", null, null, null, null, null, null));
        PatientEntity verified = patientRepository.save(new PatientEntity(
                "DPU-VERIFIED", "PAT-VERIFIED", "Nom vérifié", "FEMININ",
                LocalDate.of(1992, 2, 2), "+237600000002", "Douala", null, null, null, null, null, null));

        jdbcTemplate.update(
                "UPDATE patients SET identity_status = 'DECLARED', temporary_patient_number = 'URG-TEMP-DECLARED' WHERE id = ?",
                declared.getId());

        assertTrue(patientRepository.searchPatientsGlobally("Nom déclaré").isEmpty());
        assertEquals(verified.getId(), patientRepository.searchPatientsGlobally("Nom vérifié").getFirst().getId());
    }
}
