package ru.aigul.mts_service.migration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile("oracle")
public class BalanceMigrationRunner {

    private final XADataSource pgXaDataSource;
    private final XADataSource oracleXaDataSource;

    public BalanceMigrationRunner(@Qualifier("pgXaDataSource") XADataSource pgXaDataSource,
                                  @Qualifier("oracleXaDataSource") XADataSource oracleXaDataSource) {
        this.pgXaDataSource = pgXaDataSource;
        this.oracleXaDataSource = oracleXaDataSource;
    }

    @Value("${MIGRATE_BALANCES:false}")
    private boolean migrateBalancesEnabled;

    @Value("${MIGRATE_FAIL:false}")
    private boolean simulateFailure;

    @Value("${spring.datasource.url}")
    private String pgUrl;

    @Value("${spring.datasource.username}")
    private String pgUser;

    @Value("${spring.datasource.password}")
    private String pgPassword;

    @Value("${spring.datasource.oracle.url}")
    private String oracleUrl;

    @Value("${spring.datasource.oracle.username}")
    private String oracleUser;

    @Value("${spring.datasource.oracle.password}")
    private String oraclePassword;

    @PostConstruct
    public void runMigration() {
        if (!migrateBalancesEnabled) {
            log.info("Balance migration disabled (MIGRATE_BALANCES not set to true). Set MIGRATE_BALANCES=true to enable.");
            return;
        }

        try {
            migrateBalances();
        } catch (Exception e) {
            log.error("Balance migration failed: {}", e.getMessage(), e);
        }
    }

    public void migrateBalances() throws Exception {
        log.info("Starting programmatic JTA migration of balances from Postgres to Oracle...");

        jakarta.transaction.UserTransaction ut = com.arjuna.ats.jta.UserTransaction.userTransaction();
        jakarta.transaction.Transaction tx = com.arjuna.ats.jta.TransactionManager.transactionManager().getTransaction();

        XAConnection pgXaConn = null;
        XAConnection oracleXaConn = null;

        try {
            XADataSource pgXa = this.pgXaDataSource;
            XADataSource oracleXa = this.oracleXaDataSource;

            ut.begin();

            pgXaConn = pgXa.getXAConnection();
            XAResource pgXaRes = pgXaConn.getXAResource();
            tx.enlistResource(pgXaRes);

            oracleXaConn = oracleXa.getXAConnection();
            XAResource oracleXaRes = oracleXaConn.getXAResource();
            tx.enlistResource(oracleXaRes);

            Connection pgConn = pgXaConn.getConnection();
            Connection oracleConn = oracleXaConn.getConnection();

            List<Long> ids = new ArrayList<>();
            List<BalanceRow> rows = new ArrayList<>();
            try (PreparedStatement ps = pgConn.prepareStatement("SELECT id, user_id, amount FROM balances")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        long userId = rs.getLong("user_id");
                        java.math.BigDecimal amount = rs.getBigDecimal("amount");
                        ids.add(id);
                        rows.add(new BalanceRow(id, userId, amount));
                    }
                }
            }

            if (rows.isEmpty()) {
                log.info("No balances to migrate");
                ut.commit();
                return;
            }

            log.info("Migrating {} rows", rows.size());

            try (PreparedStatement ins = oracleConn.prepareStatement("INSERT INTO BALANCE (USER_ID, AMOUNT) VALUES (?, ?)")) {
                for (BalanceRow r : rows) {
                    ins.setLong(1, r.userId);
                    ins.setBigDecimal(2, r.amount);
                    ins.addBatch();
                }
                ins.executeBatch();
            }

            if (simulateFailure) {
                throw new RuntimeException("Simulated failure after Oracle insert (testing 2PC rollback)");
            }

            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append(ids.get(i));
                }
                try (PreparedStatement del2 = pgConn.prepareStatement("DELETE FROM balances WHERE id IN (" + sb.toString() + ")")) {
                    del2.executeUpdate();
                }
            } catch (Exception e) {
                throw e;
            }

            ut.commit();

            log.info("Migration committed successfully ({} rows)", rows.size());

        } catch (Exception ex) {
            log.error("Migration failed, attempting rollback: {}", ex.getMessage(), ex);
            try {
                com.arjuna.ats.jta.UserTransaction.userTransaction().rollback();
            } catch (Exception rbEx) {
                log.error("Rollback failed: {}", rbEx.getMessage(), rbEx);
            }
            throw ex;
        } finally {
            try {
                if (oracleXaConn != null) oracleXaConn.close();
            } catch (Exception ignored) {}
            try {
                if (pgXaConn != null) pgXaConn.close();
            } catch (Exception ignored) {}
        }
    }

    private static class BalanceRow {
        final long id;
        final long userId;
        final java.math.BigDecimal amount;

        BalanceRow(long id, long userId, java.math.BigDecimal amount) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
        }
    }
}
