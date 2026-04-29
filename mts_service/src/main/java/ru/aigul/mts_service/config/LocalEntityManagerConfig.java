package ru.aigul.mts_service.config;

import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LocalEntityManagerConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean(name = "localDataSource")
    public DataSource localDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean(name = "localEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean localEntityManagerFactory(DataSource localDataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(localDataSource);
        em.setPackagesToScan("ru.aigul.mts_service.model");
        em.setPersistenceUnitName("localPU");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.transactionType", "RESOURCE_LOCAL");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.hbm2ddl.auto", "none");
        props.put(AvailableSettings.IMPLICIT_NAMING_STRATEGY, new ImplicitNamingStrategyJpaCompliantImpl());
        props.put(AvailableSettings.PHYSICAL_NAMING_STRATEGY, new SnakeCasePhysicalNamingStrategy());
        em.setJpaPropertyMap(props);
        return em;
    }

    @Bean(name = "localTransactionManager")
    public PlatformTransactionManager localTransactionManager(LocalContainerEntityManagerFactoryBean localEntityManagerFactory) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(localEntityManagerFactory.getObject());
        return tm;
    }

    @Bean(name = "localTransactionTemplate")
    public org.springframework.transaction.support.TransactionTemplate localTransactionTemplate(@org.springframework.beans.factory.annotation.Qualifier("localTransactionManager") PlatformTransactionManager localTransactionManager) {
        return new org.springframework.transaction.support.TransactionTemplate(localTransactionManager);
    }
}
