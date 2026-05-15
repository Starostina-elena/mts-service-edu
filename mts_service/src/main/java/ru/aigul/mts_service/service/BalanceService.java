package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.balance.model.Balance;
import ru.aigul.mts_service.balance.repository.BalanceRepository;
import ru.aigul.mts_service.model.User;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserService userService;
    private final BalanceRepository balanceRepository;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Value("${payments.base-url}")
    private String paymentsBaseUrl;

    @Value("${app.currency.code}")
    private String currencyCode;

    @Transactional(readOnly = true, transactionManager = "balanceTransactionManager")
    public Optional<Balance> findBalanceForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        Optional<Balance> balOpt = balanceRepository.findByUserId(user.getId());
        if (balOpt.isPresent()) return balOpt;

        // Fallback: read from primary DB (seeded by Flyway) if not present in balance DB
        try {
            JdbcTemplate jt = new JdbcTemplate(primaryDataSource);
            Balance b = jt.queryForObject(
                    "SELECT id, user_id, amount, updated_at FROM balances WHERE user_id = ?",
                    new Object[]{user.getId()},
                    (rs, rowNum) -> {
                        Balance bb = new Balance();
                        bb.setId(rs.getLong("id"));
                        bb.setUserId(rs.getLong("user_id"));
                        bb.setAmount(rs.getBigDecimal("amount"));
                        java.sql.Timestamp t = rs.getTimestamp("updated_at");
                        if (t != null) bb.setUpdatedAt(t.toLocalDateTime());
                        return bb;
                    }
            );
            return Optional.ofNullable(b);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<String> createTopUpPayment(String email, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        String paymentId = UUID.randomUUID().toString();
        String base = paymentsBaseUrl;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String paymentUrl = base + paymentId;

        log.debug("PaymentUrl: {}", paymentUrl);

        return Optional.of(paymentUrl);
    }

    @Transactional(readOnly = true, transactionManager = "balanceTransactionManager")
    public BalanceResponse getBalanceResponseForUserEmail(String email) {
        Optional<Balance> balanceOpt = findBalanceForUserEmail(email);

        if (balanceOpt.isEmpty()) {
            return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        }

        Balance b = balanceOpt.get();
        OffsetDateTime updated = b.getUpdatedAt() != null ? b.getUpdatedAt().atOffset(ZoneOffset.UTC) : OffsetDateTime.now(ZoneOffset.UTC);
        BigDecimal amount = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amount, currencyCode, updated);
    }

    @Transactional(transactionManager = "balanceTransactionManager")
    public void applyTopUp(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Balance balance = balanceRepository.findByUserId(userId).orElseGet(() -> {
            Balance b = new Balance();
            b.setUserId(userId);
            b.setAmount(BigDecimal.ZERO);
            return b;
        });
        balance.setAmount(balance.getAmount().add(amount));
        balanceRepository.save(balance);
    }
}