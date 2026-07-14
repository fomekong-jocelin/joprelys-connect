package com.joprelys.backend.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.joprelys.backend.auth.rbac.RbacStore;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
class FlywayPostgresqlMigrationTest {

    private static final List<NumericColumnExpectation> V55_COLUMNS = List.of(
            new NumericColumnExpectation("invoices", "total_amount", 19, 4),
            new NumericColumnExpectation("invoices", "patient_share", 19, 4),
            new NumericColumnExpectation("invoices", "insurance_share", 19, 4),
            new NumericColumnExpectation("invoices", "discount_amount", 19, 4),
            new NumericColumnExpectation("invoice_items", "unit_price", 19, 4),
            new NumericColumnExpectation("invoice_items", "quantity", 19, 4),
            new NumericColumnExpectation("invoice_items", "coefficient", 19, 4),
            new NumericColumnExpectation("invoice_items", "total_item_amount", 19, 4),
            new NumericColumnExpectation("payments", "amount", 19, 4),
            new NumericColumnExpectation("receivables", "total_amount", 19, 4),
            new NumericColumnExpectation("receivables", "paid_amount", 19, 4),
            new NumericColumnExpectation("tariff_grid", "unit_value", 19, 4),
            new NumericColumnExpectation("insurance_conventions", "coverage_percentage", 5, 4));

    private static final List<String> MEDICO_LEGAL_TABLES = List.of(
            "emergency_third_parties",
            "emergency_third_party_qualities",
            "emergency_identity_statements",
            "emergency_capacity_events",
            "emergency_legal_bases",
            "emergency_legal_basis_acts",
            "emergency_belongings",
            "emergency_belonging_transfers");

    private static final List<String> PATIENT_RECONCILIATION_TABLES = List.of(
            "patient_reconciliation_events",
            "patient_canonical_links",
            "patient_identity_aliases");

    private static final List<String> RECONCILIATION_RESTRICTED_FOREIGN_KEYS = List.of(
            "fk_patient_reconciliation_corrected_event",
            "fk_patient_reconciliation_event_source",
            "fk_patient_reconciliation_event_candidate",
            "fk_patient_reconciliation_event_actor",
            "fk_patient_canonical_link_source",
            "fk_patient_canonical_link_target",
            "fk_patient_canonical_link_event",
            "fk_patient_identity_alias_origin",
            "fk_patient_identity_alias_canonical");

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_migration_test")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldApplyAllMigrationsAndSeedRbacCatalogOnPostgresql() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        flyway.migrate();
        flyway.validate();

        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Flyway doit exposer la migration courante");
        assertNotNull(current.getVersion(), "La migration courante doit être versionnée");
        assertTrue(Integer.parseInt(current.getVersion().getVersion()) >= 67,
                "Toutes les migrations de révocation persistante doivent être appliquées");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        new RbacStore(jdbcTemplate).seedCatalog();

        Integer permissionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
        Integer systemRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE system_role = TRUE", Integer.class);
        Integer sessionPermissionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE code = 'AUTH_SESSION_MANAGE'", Integer.class);
        Integer clinicAdminSessionPermissionCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.code = 'ADMIN_CLINIQUE'
                  AND rp.permission_code = 'AUTH_SESSION_MANAGE'
                """, Integer.class);
        assertNotNull(permissionCount);
        assertNotNull(systemRoleCount);
        assertTrue(permissionCount > 0, "Le catalogue des permissions doit être initialisé sur PostgreSQL");
        assertTrue(systemRoleCount > 0, "Le catalogue des rôles système doit être initialisé sur PostgreSQL");
        assertEquals(1, sessionPermissionCount);
        assertEquals(1, clinicAdminSessionPermissionCount);

        assertTableExists(jdbcTemplate, "patient_identity_declarations");
        assertTableExists(jdbcTemplate, "patient_identity_status_history");
        assertTableExists(jdbcTemplate, "emergency_triage_assessments");
        assertTableExists(jdbcTemplate, "auth_sessions");
        assertTableExists(jdbcTemplate, "revoked_access_tokens");
        assertTableExists(jdbcTemplate, "auth_session_audit_events");
        MEDICO_LEGAL_TABLES.forEach(tableName -> assertTableExists(jdbcTemplate, tableName));
        PATIENT_RECONCILIATION_TABLES.forEach(tableName -> assertTableExists(jdbcTemplate, tableName));

        RECONCILIATION_RESTRICTED_FOREIGN_KEYS.forEach(
                constraintName -> assertForeignKeyDeleteRule(jdbcTemplate, constraintName, "NO ACTION"));
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_emergency_triage_assessment_emergency",
                "CASCADE");
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_auth_sessions_replacement",
                "SET NULL");
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_auth_sessions_revoked_by_user",
                "SET NULL");
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_revoked_access_tokens_user",
                "SET NULL");
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_revoked_access_tokens_organization",
                "SET NULL");
        assertForeignKeyDeleteRule(
                jdbcTemplate,
                "fk_revoked_access_tokens_actor",
                "SET NULL");

        Integer thirdPartyColumnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'emergencies'
                  AND column_name IN (
                    'third_party_name',
                    'third_party_phone',
                    'third_party_relationship',
                    'third_party_id_document',
                    'third_party_circumstances',
                    'third_party_consent_to_contact'
                  )
                """, Integer.class);
        assertEquals(6, thirdPartyColumnCount);

        try (Connection connection = DriverManager.getConnection(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())) {
            for (NumericColumnExpectation expectation : V55_COLUMNS) {
                assertNumericColumn(connection, expectation);
            }
            assertColumnNullability(connection, "patients", "full_name", "YES");
            assertColumnNullability(connection, "patients", "gender", "YES");
            assertColumnNullability(connection, "patients", "birth_date", "YES");
            assertColumnNullability(connection, "patients", "city", "YES");
            assertColumnNullability(
                    connection,
                    "patient_reconciliation_events",
                    "evidence_source_type",
                    "NO");
            assertColumnNullability(
                    connection,
                    "patient_reconciliation_events",
                    "created_by_user_id",
                    "NO");
            assertColumnNullability(
                    connection,
                    "emergency_triage_assessments",
                    "assessment_type",
                    "NO");
            assertColumnNullability(
                    connection,
                    "auth_sessions",
                    "refresh_token_hash",
                    "NO");
            assertColumnNullability(
                    connection,
                    "auth_sessions",
                    "revoked_by_user_id",
                    "YES");
            assertColumnNullability(
                    connection,
                    "auth_sessions",
                    "revocation_source",
                    "YES");
            assertColumnNullability(
                    connection,
                    "revoked_access_tokens",
                    "token_id",
                    "NO");
            assertColumnNullability(
                    connection,
                    "auth_session_audit_events",
                    "target_user_id",
                    "NO");
        }
    }

    private static void assertTableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """, Integer.class, tableName);
        assertEquals(1, tableCount, () -> "Table introuvable : " + tableName);
    }

    private static void assertForeignKeyDeleteRule(
            JdbcTemplate jdbcTemplate,
            String constraintName,
            String expectedDeleteRule) {
        String deleteRule = jdbcTemplate.queryForObject("""
                SELECT delete_rule
                FROM information_schema.referential_constraints
                WHERE constraint_schema = 'public'
                  AND constraint_name = ?
                """, String.class, constraintName);
        assertEquals(expectedDeleteRule, deleteRule, () -> "Règle de suppression incorrecte : " + constraintName);
    }

    private static void assertNumericColumn(Connection connection, NumericColumnExpectation expectation)
            throws SQLException {
        String sql = """
                SELECT data_type, numeric_precision, numeric_scale
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, expectation.tableName());
            statement.setString(2, expectation.columnName());
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), () -> "Colonne introuvable : "
                        + expectation.tableName() + "." + expectation.columnName());
                assertEquals("numeric", resultSet.getString("data_type"));
                assertEquals(expectation.precision(), resultSet.getInt("numeric_precision"));
                assertEquals(expectation.scale(), resultSet.getInt("numeric_scale"));
            }
        }
    }

    private static void assertColumnNullability(
            Connection connection,
            String tableName,
            String columnName,
            String expectedNullability) throws SQLException {
        String sql = """
                SELECT is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            statement.setString(2, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), () -> "Colonne introuvable : " + tableName + "." + columnName);
                assertEquals(expectedNullability, resultSet.getString("is_nullable"));
            }
        }
    }

    private record NumericColumnExpectation(
            String tableName,
            String columnName,
            int precision,
            int scale) {
    }
}
