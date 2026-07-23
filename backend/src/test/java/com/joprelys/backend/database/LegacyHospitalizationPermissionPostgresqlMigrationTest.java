package com.joprelys.backend.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class LegacyHospitalizationPermissionPostgresqlMigrationTest {

    private static final String LEGACY_PERMISSION = "HOSPITALIZATION_MANAGE";

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_rbac_legacy_cleanup")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldRemoveLegacyPermissionAndCustomRoleLinkWithoutGrantingReplacementPermissions() {
        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .target("85")
                .cleanDisabled(true)
                .load()
                .migrate();

        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(),
                POSTGRESQL.getUsername(),
                POSTGRESQL.getPassword()));

        Instant now = Instant.parse("2026-07-23T00:00:00Z");
        Timestamp timestamp = Timestamp.from(now);
        UUID organizationId = UUID.randomUUID();
        UUID customRoleId = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO organizations(id, name, email, city, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                organizationId,
                "Clinique migration RBAC",
                "rbac-migration-" + organizationId + "@test.local",
                "Douala",
                timestamp,
                timestamp);
        jdbc.update(
                "INSERT INTO permissions(code, domain, name, description, created_at) VALUES (?, ?, ?, ?, ?)",
                LEGACY_PERMISSION,
                "HOSPITALISATION",
                "Permission hospitalisation historique",
                "Fixture V85 pour valider le nettoyage V86",
                timestamp);
        jdbc.update(
                "INSERT INTO roles(id, organization_id, code, name, description, system_role, assignable, enabled, created_at, updated_at, version) "
                        + "VALUES (?, ?, ?, ?, ?, FALSE, TRUE, TRUE, ?, ?, 0)",
                customRoleId,
                organizationId,
                "ROLE_HOSPITALISATION_LEGACY",
                "Rôle hospitalisation legacy",
                "Fixture de migration",
                timestamp,
                timestamp);
        jdbc.update(
                "INSERT INTO role_permissions(role_id, permission_code) VALUES (?, ?)",
                customRoleId,
                LEGACY_PERMISSION);

        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM permissions WHERE code = 'HOSPITALIZATION_MANAGE'"));
        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", customRoleId));

        Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .migrate();

        assertEquals(0, count(jdbc, "SELECT COUNT(*) FROM permissions WHERE code = 'HOSPITALIZATION_MANAGE'"));
        assertEquals(0, count(jdbc, "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", customRoleId));
        assertEquals(1, count(jdbc, "SELECT COUNT(*) FROM roles WHERE id = ?", customRoleId));
        assertEquals(0, count(jdbc, """
                SELECT COUNT(*)
                FROM role_permissions rp
                WHERE rp.role_id = ?
                  AND rp.permission_code LIKE 'HOSPITALIZATION_%'
                """, customRoleId));
        assertEquals(86, Integer.parseInt(Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .info()
                .current()
                .getVersion()
                .getVersion()));
    }

    private static int count(JdbcTemplate jdbc, String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
