package ru.aigul.mts_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.balance.repository.BalanceRepository;
import ru.aigul.mts_service.oracle.repository.OracleBalanceRepository;
import ru.aigul.mts_service.model.Balance;
import ru.aigul.mts_service.oracle.model.BalanceOracle;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class DistributedTransactionService {

    private final BalanceRepository pgBalanceRepo;
    private final Optional<OracleBalanceRepository> oracleBalanceRepo;

    public DistributedTransactionService(BalanceRepository pgBalanceRepo, Optional<OracleBalanceRepository> oracleBalanceRepo) {
        this.pgBalanceRepo = pgBalanceRepo;
        this.oracleBalanceRepo = oracleBalanceRepo;
    }

    @Transactional
    public void transferBetweenSystems(Long userId, BigDecimal amount) {
        if (oracleBalanceRepo.isEmpty()) {
            throw new IllegalStateException("Oracle balance store is not configured");
        }

        Optional<Balance> pgOpt = pgBalanceRepo.findByUserIdForUpdate(userId);
        Optional<BalanceOracle> oraOpt = oracleBalanceRepo.get().findByUserIdForUpdate(userId);

        if (pgOpt.isEmpty() || oraOpt.isEmpty()) {
            throw new IllegalArgumentException("Balance not found in one of systems for userId=" + userId);
        }

        Balance pg = pgOpt.get();
        BalanceOracle oa = oraOpt.get();

        if (oa.getAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in Oracle balance");
        }

        oa.setAmount(oa.getAmount().subtract(amount));
        pg.setAmount(pg.getAmount().add(amount));

        oracleBalanceRepo.get().save(oa);
        pgBalanceRepo.save(pg);

    }

    @Transactional
    public void transferFromPrimaryToOracle(Long userId, BigDecimal amount) {
        if (oracleBalanceRepo.isEmpty()) {
            throw new IllegalStateException("Oracle balance store is not configured");
        }

        Optional<Balance> pgOpt = pgBalanceRepo.findByUserIdForUpdate(userId);
        Optional<BalanceOracle> oraOpt = oracleBalanceRepo.get().findByUserIdForUpdate(userId);

        if (pgOpt.isEmpty() || oraOpt.isEmpty()) {
            throw new IllegalArgumentException("Balance not found in one of systems for userId=" + userId);
        }

        Balance pg = pgOpt.get();
        BalanceOracle oa = oraOpt.get();

        if (pg.getAmount().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds in primary (Postgres) balance");
        }

        pg.setAmount(pg.getAmount().subtract(amount));
        if (oa.getAmount() == null) {
            oa.setAmount(amount);
        } else {
            oa.setAmount(oa.getAmount().add(amount));
        }

        pgBalanceRepo.save(pg);
        oracleBalanceRepo.get().save(oa);
    }
}
