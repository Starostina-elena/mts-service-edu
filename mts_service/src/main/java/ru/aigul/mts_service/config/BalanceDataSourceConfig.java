package ru.aigul.mts_service.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableJpaRepositories(
        basePackages = "ru.aigul.mts_service.balance.repository",
        entityManagerFactoryRef = "balanceEntityManager",
        transactionManagerRef = "balanceTransactionManager"
)
public class BalanceDataSourceConfig {

    private static final Logger LOGGER = Logger.getLogger(BalanceDataSourceConfig.class.getName());

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
            if (balanceUrl != null) {
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
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse SPRING_DATASOURCE_BALANCE_URL='" + balanceUrl + "'", e);
        }
        ds.setUser(balanceUser);
        ds.setPassword(balancePassword);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean balanceEntityManager(@Qualifier("balanceDataSource") DataSource balanceDataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPackagesToScan("ru.aigul.mts_service.balance.model");
        emf.setPersistenceUnitName("balancePU");
        emf.setJtaDataSource(balanceDataSource);
        emf.setDataSource(balanceDataSource);
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String,Object> props = new HashMap<>();
        props.put("jakarta.persistence.transactionType", "JTA");
        props.put("hibernate.transaction.jta.platform", "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform");
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("jakarta.persistence.jdbc.url", balanceUrl);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
        props.put("jakarta.persistence.jdbc.user", balanceUser);
        props.put("jakarta.persistence.jdbc.password", balancePassword);
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Bean(name = "balanceTransactionManager")
    public PlatformTransactionManager balanceTransactionManager(@Qualifier("jtaTransactionManager") PlatformTransactionManager jta) {
        return jta;
    }
}
