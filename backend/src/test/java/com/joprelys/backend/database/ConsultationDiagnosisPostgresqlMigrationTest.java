package com.joprelys.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class ConsultationDiagnosisPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_consultation_diagnosis")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldArchiveLegacyValuesAndKeepOnlyTheMostAdvancedDiagnosis() {
        flyway("108").migrate();

        JdbcTemplate jdbc = jdbc();
        Timestamp now = Timestamp.from(Instant.now());
        UUID organizationId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID consultationId = UUID.randomUUID();

        insertClinicalContext(jdbc, organizationId, doctorId, patientId, visitId, now);
        jdbc.update("""
                INSERT INTO consultations (
                    id, visit_id, organization_id, doctor_id, document_number,
                    symptoms, clinical_exam, diagnosis, suspected_diagnosis,
                    final_diagnosis, conclusion, advice, follow_up, status,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'DOC-MIGRATION-001', 'Douleur', 'Examen',
                    'Diagnostic initial', 'Hypothèse initiale', 'Diagnostic retenu',
                    'Conclusion', 'Conseils', 'Suivi', 'BROUILLON', ?, ?)
                """, consultationId, visitId, organizationId, doctorId, now, now);

        flyway(null).migrate();

        assertThat(jdbc.queryForObject(
                "SELECT diagnosis FROM consultations WHERE id = ?",
                String.class,
                consultationId)).isEqualTo("Diagnostic retenu");
        assertThat(columnExists(jdbc, "consultations", "suspected_diagnosis")).isFalse();
        assertThat(columnExists(jdbc, "consultations", "final_diagnosis")).isFalse();
        assertThat(jdbc.queryForMap(
                "SELECT * FROM consultation_diagnosis_migration_archive WHERE consultation_id = ?",
                consultationId))
                .containsEntry("previous_diagnosis", "Diagnostic initial")
                .containsEntry("previous_suspected_diagnosis", "Hypothèse initiale")
                .containsEntry("previous_final_diagnosis", "Diagnostic retenu");
    }

    private void insertClinicalContext(
            JdbcTemplate jdbc,
            UUID organizationId,
            UUID doctorId,
            UUID patientId,
            UUID visitId,
            Timestamp now) {
        jdbc.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, 'Clinique migration', ?, '000', 'Adresse test',
                    'Douala', 'ACTIVE', 'CLINIC', 'Cameroun', 'Responsable', TRUE, ?, ?)
                """, organizationId, "migration-" + organizationId + "@joprelys.local", now, now);
        jdbc.update("""
                INSERT INTO users (
                    id, email, display_name, role, password_hash, enabled,
                    organization_id, created_at, updated_at
                ) VALUES (?, ?, 'Dr Migration', 'MEDECIN', 'hash', TRUE, ?, ?, ?)
                """, doctorId, "doctor-" + doctorId + "@joprelys.local", organizationId, now, now);
        jdbc.update("""
                INSERT INTO patients (
                    id, organization_id, global_patient_number, local_patient_number,
                    full_name, gender, birth_date, phone, city, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'Patient Migration', 'MASCULIN', ?, '000',
                    'Douala', 'ACTIVE', ?, ?)
                """, patientId, organizationId, "DPU-" + patientId, "PAT-" + patientId,
                LocalDate.of(1990, 1, 1), now, now);
        jdbc.update("""
                INSERT INTO visits (
                    id, organization_id, patient_id, visit_number, reason,
                    orientation, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'Consultation', 'GENERAL', 'EN_COURS', ?, ?)
                """, visitId, organizationId, patientId, "VIS-" + visitId, now, now);
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true);
        if (target != null) configuration.target(target);
        return configuration.load();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));
    }

    private boolean columnExists(JdbcTemplate jdbc, String tableName, String columnName) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """, Long.class, tableName, columnName);
        return count != null && count > 0;
    }
}
