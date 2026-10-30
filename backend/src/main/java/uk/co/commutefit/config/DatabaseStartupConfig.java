package uk.co.commutefit.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.context.annotation.Profile("legacy")
public class DatabaseStartupConfig {
    @Bean
    FlywayMigrationStrategy safeFlywayMigrationStrategy() {
        return (Flyway flyway) -> {
            try {
                flyway.migrate();
            } catch (RuntimeException ex) {
                throw new IllegalStateException(
                    "Commute Fit could not connect to or migrate its PostgreSQL database. " +
                    "No reset or destructive recovery was attempted; check the local database settings and retry.", ex);
            }
        };
    }
}
