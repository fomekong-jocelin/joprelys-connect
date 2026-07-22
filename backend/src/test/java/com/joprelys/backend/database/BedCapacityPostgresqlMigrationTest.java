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
class BedCapacityPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_bed_capacity_test")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldBackfillAxesAndRejectAnIncoherentLegacyProjection() {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));

        assertEquals(2, bedAxisColumnCount(jdbcTemplate));
        assertEquals(1, constraintCount(jdbcTemplate, "ck_beds_capacity_status"));
        assertEquals(1, constraintCount(jdbcTemplate, "ck_beds_readiness_status"));
        assertEquals(1, constraintCount(jdbcTemplate, "ck_beds_legacy_status_projection"));

        UUID bedId = insertFreeBed(jdbcTemplate);
        assertEquals("OPEN", columnValue(jdbcTemplate, bedId, "capacity_status"));
        assertEquals("READY", columnValue(jdbcTemplate, bedId, "readiness_status"));

        jdbcTemplate.update("""
                UPDATE beds
                SET capacity_status = 'CLOSED',
                    status = 'MAINTENANCE'
                WHERE id = ?
                """, bedId);
        assertEquals("CLOSED", columnValue(jdbcTemplate, bedId, "capacity_status"));
        assertEquals("READY", columnValue(jdbcTemplate, bedId, "readiness_status"));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        UPDATE beds
                        SET status = 'FREE'
                        WHERE id = ?
                        """, bedId),
                "Un lit fermé ne doit pas pouvoir rester projeté comme FREE");
    }

    private int bedAxisColumnCount(JdbcTemplate jdbcTemplate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'beds'
                  AND column_name IN ('capacity_status', 'readiness_status')
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private int constraintCount(JdbcTemplate jdbcTemplate, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_constraint
                WHERE conname = ?
                """, Integer.class, constraintName);
        return count == null ? 0 : count;
    }

    private UUID insertFreeBed(JdbcTemplate jdbcTemplate) {
        UUID organizationId = UUID.randomUUID();
        UUID wardId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());

        jdbcTemplate.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, 'Clinique capacité', ?, '000', 'Adresse', 'Douala',
                    'ACTIVE', 'CLINIC', 'Cameroun', 'Responsable', TRUE, ?, ?)
                """, organizationId, "capacity-" + organizationId + "@joprelys.local", now, now);
        jdbcTemplate.update("""
                INSERT INTO wards (id, name, organization_id, created_at, updated_at, service_type)
                VALUES (?, 'Hospitalisation', ?, ?, ?, 'HOSPITALIZATION')
                """, wardId, organizationId, now, now);
        jdbcTemplate.update("""
                INSERT INTO rooms (
                    id, ward_id, room_number, capacity, comfort_level, organization_id, created_at, updated_at
                ) VALUES (?, ?, '301', 1, 'STANDARD', ?, ?, ?)
                """, roomId, wardId, organizationId, now, now);
        jdbcTemplate.update("""
                INSERT INTO beds (
                    id, room_id, bed_number, status, organization_id, version, created_at, updated_at
                ) VALUES (?, ?, '301-A', 'FREE', ?, 0, ?, ?)
                """, bedId, roomId, organizationId, now, now);
        return bedId;
    }

    private String columnValue(JdbcTemplate jdbcTemplate, UUID bedId, String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT " + columnName + " FROM beds WHERE id = ?",
                String.class,
                bedId);
    }
}
