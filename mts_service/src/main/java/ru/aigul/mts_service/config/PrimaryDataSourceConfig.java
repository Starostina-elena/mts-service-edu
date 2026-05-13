package ru.aigul.mts_service.config;

import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jta.narayana.NarayanaXADataSourceWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "ru.aigul.mts_service.repository",
        entityManagerFactoryRef = "primaryEntityManager",
        transactionManagerRef = "transactionManager"
)
public class PrimaryDataSourceConfig {

    @Autowired
    private NarayanaXADataSourceWrapper xaWrapper;

    @Value("${app.datasource.primary.url}")
    private String url;
    @Value("${app.datasource.primary.username}")
    private String username;
    @Value("${app.datasource.primary.password}")
    private String password;

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        PGXADataSource xa = new PGXADataSource();
        xa.setUrl(url);
        xa.setUser(username);
        xa.setPassword(password);
        return xaWrapper.wrapDataSource(xa);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean primaryEntityManager() {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(primaryDataSource());
        emf.setPackagesToScan("ru.aigul.mts_service.model");
        emf.setPersistenceUnitName("primaryPU");
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.transaction.jta.platform",
                "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform");
        props.put("jakarta.persistence.transactionType", "JTA");
        emf.setJpaPropertyMap(props);
        return emf;
    }
}
