package ru.aigul.mts_service.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jta.narayana.NarayanaXADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.XADataSource;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(basePackages = "ru.aigul.mts_service.oracle.repository",
        entityManagerFactoryRef = "balanceEntityManagerFactory",
        transactionManagerRef = "transactionManager")
public class BalanceJpaConfig {

    @Value("${spring.datasource.balance.url}")
    private String url;

    @Value("${spring.datasource.balance.username}")
    private String username;

    @Value("${spring.datasource.balance.password}")
    private String password;

    @Bean(name = "balanceXaDataSource")
    public XADataSource balanceXaDataSource() {
        PGXADataSource xa = new PGXADataSource();
        try { xa.setUrl(url); } catch (Exception ignored) {}
        try { xa.setUser(username); } catch (Exception ignored) {}
        try { xa.setPassword(password); } catch (Exception ignored) {}
        return xa;
    }

    @Bean(name = "balanceDataSource")
    public DataSource balanceDataSource(NarayanaXADataSourceWrapper wrapper) {
        return wrapper.wrapDataSource(balanceXaDataSource());
    }

    @Bean(name = "balanceEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean balanceEntityManagerFactory(@Qualifier("balanceDataSource") DataSource ds) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ds);
        em.setPackagesToScan("ru.aigul.mts_service.oracle.model");
        em.setPersistenceUnitName("balancePU");

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
