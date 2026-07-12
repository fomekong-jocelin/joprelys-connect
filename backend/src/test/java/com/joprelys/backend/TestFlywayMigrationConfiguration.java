package com.joprelys.backend;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Applies migrations that exist only to isolate the shared H2 integration-test database.
 *
 * <p>The cleanup policy uses a separate Flyway history table, so the direct PostgreSQL
 * migration test continues to validate only the production migration location.</p>
 */
@Configuration(proxyBeanMethods = false)
public class TestFlywayMigrationConfiguration {

    @Bean
    ApplicationRunner applyTestCleanupMigrations(DataSource dataSource) {
        return arguments -> Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/test-migration")
                .table("flyway_test_schema_history")
                .baselineOnMigrate(true)
                .load()
                .migrate();
    }
}