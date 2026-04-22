package ru.aigul.mts_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.SQLException;

@Configuration
@ConditionalOnClass(name = "oracle.jdbc.xa.client.OracleXADataSource")
public class OracleDataSourceConfig {

    @Value("${spring.datasource.oracle.url}")
    private String url;

    @Value("${spring.datasource.oracle.username}")
    private String user;

    @Value("${spring.datasource.oracle.password}")
    private String password;

    @Bean(name = "oracleDriverDataSource")
    public DataSource oracleDriverDataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("oracle.jdbc.OracleDriver");
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);
        return ds;
    }

    @Bean(name = "oracleXaDataSource")
    public XADataSource oracleXaDataSource() throws SQLException {
        try {
            Class<?> oracleXaClass = Class.forName("oracle.jdbc.xa.client.OracleXADataSource");
            Object xaInstance = oracleXaClass.getDeclaredConstructor().newInstance();

            try {
                oracleXaClass.getMethod("setURL", String.class).invoke(xaInstance, url);
            } catch (NoSuchMethodException ignored) {}
            try {
                oracleXaClass.getMethod("setUser", String.class).invoke(xaInstance, user);
            } catch (NoSuchMethodException ignored) {}
            try {
                oracleXaClass.getMethod("setPassword", String.class).invoke(xaInstance, password);
            } catch (NoSuchMethodException ignored) {}

            return (XADataSource) xaInstance;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Oracle XADataSource class not found on classpath. Put ojdbc8.jar into ./lib or add to classpath.", e);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new SQLException("Failed to create Oracle XADataSource", ex);
        }
    }
}
