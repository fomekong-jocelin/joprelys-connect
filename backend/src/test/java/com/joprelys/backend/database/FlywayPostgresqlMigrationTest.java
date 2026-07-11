package com.joprelys.backend.database;

import com.joprelys.backend.auth.rbac.RbacStore;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            new NumericColumnExpectation("insurance_conventions", "coverage_percentage", 5, 4)
    );

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
        assertTrue(Integer.parseInt(current.getVersion().getVersion()) >= 59,
                "Toutes les migrations jusqu'au tiers accompagnant doivent être appliquées");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        new RbacStore(jdbcTemplate).seedCatalog();

        Integer permissionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
        Integer systemRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE system_role = TRUE", Integer.class);
        assertNotNull(permissionCount);
        assertNotNull(systemRoleCount);
        assertTrue(permissionCount > 0, "Le catalogue des permissions doit être initialisé sur PostgreSQL");
        assertTrue(systemRoleCount > 0, "Le catalogue des rôles système doit être initialisé sur PostgreSQL");

        Integer declarationTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'patient_identity_declarations'",
                Integer.class);
        Integer historyTableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'patient_identity_status_history'",
                Integer.class);
        assertEquals(1, declarationTableCount);
        assertEquals(1, historyTableCount);

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
            assertColumnNullable(connection, "patients", "full_name");
            assertColumnNullable(connection, "patients", "gender");
            assertColumnNullable(connection, "patients", "birth_date");
            assertColumnNullable(connection, "patients", "city");
        }
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

    private static void assertColumnNullable(Connection connection, String tableName, String columnName)
            throws SQLException {
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
                assertEquals("YES", resultSet.getString("is_nullable"));
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
