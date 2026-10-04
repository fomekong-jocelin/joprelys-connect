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
class HospitalOrganizationPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_hospital_org_test")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldCreateCatalogsAndEnforceTenantAndServiceConstraints() {
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

        assertEquals(23, count(jdbc, "hospital_service_catalog"));
        assertEquals(20, count(jdbc, "medical_specialty_catalog"));
        assertEquals(4, count(jdbc, "organizational_unit_type_catalog"));
        assertEquals(1, countByCode(jdbc, "hospital_service_catalog", "GENERAL_MEDICINE"));
        assertEquals(1, countByCode(jdbc, "medical_specialty_catalog", "GENERAL_MEDICINE"));
        assertEquals(1, countByCode(jdbc, "organizational_unit_type_catalog", "SERVICE"));

        UUID organizationA = insertOrganization(jdbc, "a");
        UUID organizationB = insertOrganization(jdbc, "b");
        UUID serviceA = UUID.randomUUID();
        UUID serviceB = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());

        insertService(jdbc, serviceA, organizationA, "SVC_GENERAL", now);
        insertService(jdbc, serviceB, organizationB, "SVC_GENERAL", now);

        assertEquals(2, countByUnitCode(jdbc, "SVC_GENERAL"),
                "Le même code est autorisé dans deux tenants distincts");

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, ?, 'CARE-CROSS', 'Unité cross tenant', 'CARE_UNIT', NULL, TRUE, ?, ?)
                """, UUID.randomUUID(), organizationB, serviceA, now, now));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'SVC-NO-CATALOG', NULL, 'SERVICE', NULL, TRUE, ?, ?)
                """, UUID.randomUUID(), organizationA, now, now));

        assertEquals(1, jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'SVC-FREE-NAME', 'Chirurgie septique', 'SERVICE', 'GENERAL_MEDICINE', TRUE, ?, ?)
                """, UUID.randomUUID(), organizationA, now, now));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'POLE-CATALOG', 'Pôle invalide', 'POLE', 'GENERAL_MEDICINE', TRUE, ?, ?)
                """, UUID.randomUUID(), organizationA, now, now));

        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, 'UNKNOWN-TYPE', 'Type inconnu', 'UNKNOWN', NULL, TRUE, ?, ?)
                """, UUID.randomUUID(), organizationA, now, now));
    }

    private UUID insertOrganization(JdbcTemplate jdbc, String suffix) {
        UUID id = UUID.randomUUID();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO organizations (
                    id, name, email, phone, address, city, status, type, country,
                    responsible_name, api_enabled, created_at, updated_at
                ) VALUES (?, ?, ?, '000', 'Adresse', 'Douala', 'ACTIVE', 'CLINIC',
                    'Cameroun', 'Responsable', TRUE, ?, ?)
                """, id, "Clinique " + suffix, "org-" + suffix + "-" + id + "@joprelys.local", now, now);
        return id;
    }

    private void insertService(
            JdbcTemplate jdbc,
            UUID id,
            UUID organizationId,
            String code,
            Timestamp now) {
        jdbc.update("""
                INSERT INTO organizational_units (
                    id, organization_id, parent_id, code, name, unit_type,
                    service_catalog_code, active, created_at, updated_at
                ) VALUES (?, ?, NULL, ?, NULL, 'SERVICE', 'GENERAL_MEDICINE', TRUE, ?, ?)
                """, id, organizationId, code, now, now);
    }

    private int count(JdbcTemplate jdbc, String table) {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }

    private int countByCode(JdbcTemplate jdbc, String table, String code) {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE code = ?", Integer.class, code);
        return value == null ? 0 : value;
    }

    private int countByUnitCode(JdbcTemplate jdbc, String code) {
        Integer value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM organizational_units WHERE code = ?", Integer.class, code);
        return value == null ? 0 : value;
    }
}
