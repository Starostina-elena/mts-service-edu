package ru.aigul.mts_service.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    @Bean
    @Primary
    public Flyway flyway(@Qualifier("dataSource") DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        try {
            flyway.migrate();
        } catch (Exception e) {
            log.warn("Flyway migration failed or could not connect: {}. Application will continue to start.", e.getMessage());
        }
        return flyway;
    }
}
