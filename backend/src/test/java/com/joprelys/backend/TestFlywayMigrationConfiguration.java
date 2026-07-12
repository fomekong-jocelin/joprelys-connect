package com.joprelys.backend;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds migrations that exist only to isolate the shared H2 integration-test database.
 *
 * <p>The direct PostgreSQL migration test configures Flyway itself and therefore
 * continues to validate only the production migration location.</p>
 */
@Configuration(proxyBeanMethods = false)
public class TestFlywayMigrationConfiguration {

    @Bean
    FlywayConfigurationCustomizer testFlywayLocations() {
        return configuration -> configuration.locations(
                "classpath:db/migration",
                "classpath:db/test-migration");
    }
}