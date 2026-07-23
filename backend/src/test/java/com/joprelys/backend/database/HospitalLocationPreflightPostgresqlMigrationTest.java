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
class HospitalLocationPreflightPostgresqlMigrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("joprelys_hospital_location_preflight")
            .withUsername("joprelys")
            .withPassword("joprelys");

    @Test
    void shouldBlockBeforeAnyBreakingMutationWhenLegacySpatialDataExists() {
        Flyway baseline = flyway("87");
        baseline.migrate();

        JdbcTemplate jdbc = jdbc();
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO wards (id, name, service_type, organization_id, created_at, updated_at)
                VALUES (?, 'Legacy ward', 'HOSPITALIZATION', ?, ?, ?)
                """, UUID.randomUUID(), UUID.randomUUID(), now, now);

        Flyway latest = flyway(null);
        assertThatThrownBy(latest::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("HOS-LOC V88 blocked")
                .hasMessageContaining("wards=1")
                .hasMessageContaining("No automatic mapping is allowed");

        assertThat(count(jdbc, "SELECT COUNT(*) FROM wards")).isEqualTo(1);
        assertThat(tableExists(jdbc, "facility_spaces")).isFalse();
        assertThat(count(jdbc, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '88' AND success = TRUE"))
                .isZero();

        jdbc.update("DELETE FROM wards");
        latest.migrate();

        assertThat(count(jdbc, "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '88' AND success = TRUE"))
                .isEqualTo(1);
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
}
