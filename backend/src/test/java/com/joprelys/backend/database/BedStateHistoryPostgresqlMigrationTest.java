package com.joprelys.backend.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class BedStateHistoryPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_bed_state_history")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldEnforceTenantAndEventIntegrityOnPostgresql() {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));

        UUID organizationId = UUID.randomUUID();
        UUID otherOrganizationId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-22T07:00:00Z");
        Timestamp timestamp = Timestamp.from(now);

        insertOrganization(jdbc, organizationId, "history-main");
        insertOrganization(jdbc, otherOrganizationId, "history-other");
        jdbc.update("""
                INSERT INTO facility_spaces (
                    id, organization_id, location_node_id, code, name, space_type_code,
                    active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'HISTORY_ROOM', 'Chambre historique', 'HOSPITAL_ROOM', TRUE, ?, ?)
                """, spaceId, organizationId, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO inpatient_space_profiles (
                    space_id, organization_id, space_type_code, comfort_level, created_at, updated_at
                ) VALUES (?, ?, 'HOSPITAL_ROOM', 'STANDARD', ?, ?)
                """, spaceId, organizationId, timestamp, timestamp);
        jdbc.update("""
                INSERT INTO beds (
                    id, space_id, bed_number, status, capacity_status, readiness_status,
                    organization_id, version, created_at, updated_at
                ) VALUES (?, ?, '101-A', 'FREE', 'OPEN', 'READY', ?, 0, ?, ?)
                """, bedId, spaceId, organizationId, timestamp, timestamp);

        jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, bedId, "CAPACITY", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", timestamp);

        Integer historyCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_state_changes WHERE bed_id = ?",
                Integer.class,
                bedId);
        assertEquals(1, historyCount);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, bedId, "UNKNOWN", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", timestamp));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), otherOrganizationId, bedId, "CAPACITY", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", timestamp));

        jdbc.update("DELETE FROM beds WHERE id = ?", bedId);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_state_changes WHERE bed_id = ?",
                Integer.class,
                bedId));
    }

    private void insertOrganization(JdbcTemplate jdbc, UUID organizationId, String suffix) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, '000', 'Adresse', 'Douala', 'ACTIVE', 'CLINIC',
                    'Cameroun', 'Responsable', TRUE, ?, ?)
                """,
                organizationId,
                "Clinique " + suffix,
                suffix + "-" + organizationId + "@joprelys.local",
                now,
                now);
    }
}
