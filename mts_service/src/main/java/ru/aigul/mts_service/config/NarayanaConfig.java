package ru.aigul.mts_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.jta.narayana.NarayanaXADataSourceWrapper;

@Configuration
public class NarayanaConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager() {
        jakarta.transaction.TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        jakarta.transaction.UserTransaction ut = com.arjuna.ats.jta.UserTransaction.userTransaction();

        org.springframework.transaction.jta.JtaTransactionManager jtaTm = new org.springframework.transaction.jta.JtaTransactionManager();
        jtaTm.setTransactionManager(tm);
        jtaTm.setUserTransaction(ut);
        jtaTm.setAllowCustomIsolationLevels(true);
        return jtaTm;
    }

    @Bean
    public CommandLineRunner printTxManager(PlatformTransactionManager txManager) {
        return args -> {
            System.out.println("PlatformTransactionManager bean: " + txManager.getClass().getName());
            if (txManager instanceof org.springframework.transaction.jta.JtaTransactionManager jta) {
                Object tm = jta.getTransactionManager();
                Object ut = jta.getUserTransaction();
                System.out.println(" -> underlying TransactionManager: " + (tm != null ? tm.getClass().getName() : "<null>"));
                System.out.println(" -> underlying UserTransaction: " + (ut != null ? ut.getClass().getName() : "<null>"));
            }
            try {
                Object arjunaTm = com.arjuna.ats.jta.TransactionManager.transactionManager();
                System.out.println("Arjuna static TM: " + (arjunaTm != null ? arjunaTm.getClass().getName() : "<null>"));
            } catch (Throwable t) {
                System.out.println("Arjuna TransactionManager not available: " + t.getClass().getName() + ": " + t.getMessage());
            }
        };
    }

    @Bean
    public NarayanaXADataSourceWrapper narayanaXADataSourceWrapper() {
        return new NarayanaXADataSourceWrapper();
    }

    @Bean(destroyMethod = "shutdown")
    public java.util.concurrent.ScheduledExecutorService scheduledExecutorService() {
        return java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduled-executor");
            t.setDaemon(true);
            return t;
        });
    }
}
