package ru.aigul.mts_service.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        basePackages = "ru.aigul.mts_service.balance.repository",
        entityManagerFactoryRef = "balanceEntityManager",
        transactionManagerRef = "balanceTransactionManager"
)
public class BalanceDataSourceConfig {

    @Value("${SPRING_DATASOURCE_BALANCE_URL:jdbc:postgresql://balance_db:5432/mts_balance?stringtype=unspecified}")
    private String balanceUrl;

    @Value("${SPRING_DATASOURCE_BALANCE_USERNAME:postgres}")
    private String balanceUser;

    @Value("${SPRING_DATASOURCE_BALANCE_PASSWORD:postgres}")
    private String balancePassword;

    @Bean
    public DataSource balanceDataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        try {
            String s = balanceUrl;
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
        }
        ds.setUser(balanceUser);
        ds.setPassword(balancePassword);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean balanceEntityManager(DataSource balanceDataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPackagesToScan("ru.aigul.mts_service.balance.model");
        emf.setPersistenceUnitName("balancePU");
        // Используем локальный DataSource для этого EntityManager (совместимо с JpaTransactionManager)
        emf.setDataSource(balanceDataSource);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String,Object> props = new HashMap<>();
        props.put("jakarta.persistence.transactionType", "JTA");
        props.put("hibernate.transaction.jta.platform", "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform");
        props.put("hibernate.hbm2ddl.auto", "update");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean(name = "balanceTransactionManager")
    public PlatformTransactionManager balanceTransactionManager(LocalContainerEntityManagerFactoryBean balanceEntityManager) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(balanceEntityManager.getObject());
        return tm;
    }
}
