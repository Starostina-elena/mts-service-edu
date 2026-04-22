package ru.aigul.mts_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jta.narayana.NarayanaXADataSourceWrapper;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;

import javax.sql.XADataSource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Primary
@EnableJpaRepositories(basePackages = "ru.aigul.mts_service.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
public class PostgresJpaConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("pgXaDataSource") XADataSource pgXaDataSource, NarayanaXADataSourceWrapper wrapper) {
        return wrapper.wrapDataSource(pgXaDataSource);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("dataSource") DataSource ds) {

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ds);
        em.setPackagesToScan("ru.aigul.mts_service.model");
        em.setPersistenceUnitName("defaultPU");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.transactionType", "JTA");
        props.put("hibernate.transaction.jta.platform", "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, new ImplicitNamingStrategyJpaCompliantImpl());
        props.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, new SnakeCasePhysicalNamingStrategy());
        em.setJpaPropertyMap(props);
        return em;
    }
}
