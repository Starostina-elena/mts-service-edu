package ru.aigul.mts_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
public class JtaConfig {

    @Bean(name = "jtaTransactionManager")
    public PlatformTransactionManager jtaTransactionManager() {
        JtaTransactionManager jta = new JtaTransactionManager();
        try {
            jakarta.transaction.TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
            jakarta.transaction.UserTransaction ut = com.arjuna.ats.jta.UserTransaction.userTransaction();
            jta.setTransactionManager(tm);
            jta.setUserTransaction(ut);
        } catch (Throwable t) {
        }
        return jta;
    }
}
