package ru.aigul.mts_service.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "ru.aigul.mts_service.repository",
        entityManagerFactoryRef = "primaryEntityManager"
)
public class PrimaryXaConfig {

    private static final Logger log = LoggerFactory.getLogger(PrimaryXaConfig.class);

    @Value("${SPRING_DATASOURCE_URL:jdbc:postgresql://db:5432/mts_db?stringtype=unspecified}")
    private String primaryJdbcUrl;

    @Value("${SPRING_DATASOURCE_USERNAME:postgres}")
    private String primaryUser;

    @Value("${SPRING_DATASOURCE_PASSWORD:postgres}")
    private String primaryPassword;

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        try {
            String s = primaryJdbcUrl;
            String prefix = "jdbc:postgresql://";
            if (s.startsWith(prefix)) {
                String rest = s.substring(prefix.length());
                String hostPortDb = rest.split("\\?")[0];
                String[] parts = hostPortDb.split("/");
                String hostPort = parts[0];
                String dbname = parts.length > 1 ? parts[1] : null;
                String host = hostPort;
                int port = 5432;
                if (hostPort.contains(":")) {
                    String[] hp = hostPort.split(":" );
                    host = hp[0];
                    port = Integer.parseInt(hp[1]);
                }
                if (dbname != null) ds.setDatabaseName(dbname);
                ds.setServerNames(new String[]{host});
                ds.setPortNumbers(new int[]{port});
            }
        } catch (Exception e) {
            log.warn("Failed to parse primary JDBC URL: {}", e.getMessage());
        }
        ds.setUser(primaryUser);
        ds.setPassword(primaryPassword);
        return ds;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean primaryEntityManager(DataSource primaryDataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPackagesToScan("ru.aigul.mts_service.model");
        emf.setPersistenceUnitName("primaryPU");
        emf.setJtaDataSource(primaryDataSource);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String,Object> props = new HashMap<>();
        props.put("jakarta.persistence.transactionType", "JTA");
        props.put("hibernate.transaction.jta.platform", "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform");
        // В локальной Dev среде разрешаем Hibernate обновлять схему (создавать недостающие таблицы).
        // В проде рекомендуется управлять схемой через миграции и вернуть validate.
        props.put("hibernate.hbm2ddl.auto", "update");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean primaryEntityManager) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(primaryEntityManager.getObject());
        return tm;
    }
}
