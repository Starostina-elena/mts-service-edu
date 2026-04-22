package ru.aigul.mts_service.config;

import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.XADataSource;
import java.sql.SQLException;

@Configuration
public class XaDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String pgUrl;

    @Value("${spring.datasource.username}")
    private String pgUser;

    @Value("${spring.datasource.password}")
    private String pgPassword;

    @Bean(name = "pgXaDataSource")
    public XADataSource pgXaDataSource() throws SQLException {
        PGXADataSource pgXa = new PGXADataSource();
        try {
            pgXa.setUrl(pgUrl);
        } catch (Exception ignored) {}
        try { pgXa.setUser(pgUser); } catch (Exception ignored) {}
        try { pgXa.setPassword(pgPassword); } catch (Exception ignored) {}
        return pgXa;
    }

}
