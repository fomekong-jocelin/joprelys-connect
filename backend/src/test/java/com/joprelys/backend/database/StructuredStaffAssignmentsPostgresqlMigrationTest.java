package com.joprelys.backend.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class StructuredStaffAssignmentsPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_structured_staff")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldFailFastOnLegacyTextThenEnforceStructuredPeriods() {
        Flyway baseline = flyway("91");
        baseline.migrate();

        JdbcTemplate jdbc = jdbc();
        Timestamp now = Timestamp.from(Instant.now());
        UUID organizationId = UUID.randomUUID();
        UUID legacyStaffId = UUID.randomUUID();

        insertOrganization(jdbc, organizationId, now);
        insertLegacyUser(jdbc, legacyStaffId, organizationId, now);

        Flyway latest = flyway(null);
        assertThatThrownBy(latest::migrate)
                .isInstanceOf(FlywayException.class)
                .hasStackTraceContaining("HOS-STAFF V92 blocked")
                .hasStackTraceContaining("department=1")
                .hasStackTraceContaining("specialty=1")
                .hasStackTraceContaining("No automatic mapping by name or similarity is allowed");

        assertThat(tableExists(jdbc, "staff_specialty_assignments")).isFalse();
        assertThat(count(jdbc, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '92' AND success = TRUE"))
                .isZero();

        jdbc.update("DELETE FROM users WHERE id = ?", legacyStaffId);
        latest.migrate();

        assertThat(tableExists(jdbc, "staff_specialty_assignments")).isTrue();
        assertThat(tableExists(jdbc, "staff_organizational_unit_assignments")).isTrue();
        assertThat(columnExists(jdbc, "users", "department")).isFalse();
        assertThat(columnExists(jdbc, "users", "specialty")).isFalse();

        UUID staffId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        insertStructuredUser(jdbc, staffId, organizationId, now);
        jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'MED-GEN-STAFF', NULL, 'SERVICE', 'GENERAL_MEDICINE', TRUE, ?, ?)
                """, unitId, organizationId, now, now);

        Timestamp from = Timestamp.from(Instant.now().minusSeconds(3600));
        Timestamp to = Timestamp.from(Instant.now().plusSeconds(3600));
        jdbc.update("""
                INSERT INTO staff_specialty_assignments (
                    id, organization_id, staff_id, specialty_code, is_primary,
                    valid_from, valid_to, created_at, updated_at
                ) VALUES (?, ?, ?, 'GENERAL_MEDICINE', TRUE, ?, ?, ?, ?)
                """, UUID.randomUUID(), organizationId, staffId, from, to, now, now);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO staff_specialty_assignments (
                    id, organization_id, staff_id, specialty_code, is_primary,
                    valid_from, valid_to, created_at, updated_at
                ) VALUES (?, ?, ?, 'GENERAL_MEDICINE', FALSE, ?, NULL, ?, ?)
                """, UUID.randomUUID(), organizationId, staffId, Timestamp.from(Instant.now()), now, now))
                .hasStackTraceContaining("ex_staff_specialty_period_no_overlap");

        jdbc.update("""
                INSERT INTO staff_organizational_unit_assignments (
                    id, organization_id, staff_id, organizational_unit_id, assignment_role_code,
                    is_primary, valid_from, valid_to, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'PRACTITIONER', TRUE, ?, ?, ?, ?)
                """, UUID.randomUUID(), organizationId, staffId, unitId, from, to, now, now);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO staff_organizational_unit_assignments (
                    id, organization_id, staff_id, organizational_unit_id, assignment_role_code,
                    is_primary, valid_from, valid_to, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'PRACTITIONER', FALSE, ?, NULL, ?, ?)
                """, UUID.randomUUID(), organizationId, staffId, unitId, Timestamp.from(Instant.now()), now, now))
                .hasStackTraceContaining("ex_staff_unit_period_no_overlap");
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));
    }

    private void insertOrganization(JdbcTemplate jdbc, UUID organizationId, Timestamp now) {
        jdbc.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                organizationId,
                "Clinique legacy HOS-STAFF",
                "hos-staff-preflight-" + organizationId + "@joprelys.local",
                "000",
                "Adresse test",
                "Douala",
                "ACTIVE",
                "CLINIC",
                "Cameroun",
                "Responsable test",
                true,
                now,
                now);
    }

    private void insertLegacyUser(JdbcTemplate jdbc, UUID staffId, UUID organizationId, Timestamp now) {
        jdbc.update("""
                INSERT INTO users (
                    id, email, display_name, role, password_hash, enabled,
                    organization_id, created_at, updated_at, department, specialty
                ) VALUES (?, ?, 'Dr Legacy', 'MEDECIN', 'hash', TRUE, ?, ?, ?, 'Cardiologie libre', 'Cardiologie libre')
                """,
                staffId,
                "legacy-staff-" + staffId + "@joprelys.local",
                organizationId,
                now,
                now);
    }

    private void insertStructuredUser(JdbcTemplate jdbc, UUID staffId, UUID organizationId, Timestamp now) {
        jdbc.update("""
                INSERT INTO users (
                    id, email, display_name, role, password_hash, enabled,
                    organization_id, created_at, updated_at
                ) VALUES (?, ?, 'Dr Structured', 'MEDECIN', 'hash', TRUE, ?, ?, ?)
                """,
                staffId,
                "structured-staff-" + staffId + "@joprelys.local",
                organizationId,
                now,
                now);
    }

    private long count(JdbcTemplate jdbc, String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private boolean tableExists(JdbcTemplate jdbc, String tableName) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """, Long.class, tableName);
        return count != null && count > 0;
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
