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
        UUID wardId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-22T07:00:00Z");

        jdbc.update(
                "INSERT INTO wards(id, name, service_type, organization_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                wardId, "Médecine", "HOSPITALIZATION", organizationId, Timestamp.from(now), Timestamp.from(now));
        jdbc.update(
                "INSERT INTO rooms(id, ward_id, room_number, capacity, comfort_level, organization_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                roomId, wardId, "101", 1, "STANDARD", organizationId, Timestamp.from(now), Timestamp.from(now));
        jdbc.update(
                "INSERT INTO beds(id, room_id, bed_number, status, organization_id, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                bedId, roomId, "101-A", "FREE", organizationId, 0, Timestamp.from(now), Timestamp.from(now));

        jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, bedId, "CAPACITY", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", Timestamp.from(now));

        Integer historyCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_state_changes WHERE bed_id = ?",
                Integer.class,
                bedId);
        assertEquals(1, historyCount);

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), organizationId, bedId, "UNKNOWN", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", Timestamp.from(now)));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO bed_state_changes(id, organization_id, bed_id, state_axis, previous_value, new_value, reason_code, actor_display_name, source, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), otherOrganizationId, bedId, "CAPACITY", "OPEN", "CLOSED",
                "CAPACITY_SAFETY", "Responsable hospitalisation", "MANUAL", Timestamp.from(now)));

        jdbc.update("DELETE FROM beds WHERE id = ?", bedId);
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM bed_state_changes WHERE bed_id = ?",
                Integer.class,
                bedId));
    }
}
