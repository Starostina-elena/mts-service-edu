package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.balance.model.Balance;
import ru.aigul.mts_service.balance.repository.BalanceRepository;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.UserRepository;

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
    private final UserRepository userRepository;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    @Qualifier("balanceDataSource")
    private DataSource balanceDataSource;

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

        try {
            JdbcTemplate jt = new JdbcTemplate(balanceDataSource);
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

    @Transactional(transactionManager = "balanceTransactionManager")
    public void applyTopUp(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        Balance balance = balanceRepository.findByUserId(userId).orElseGet(() -> {
            Balance b = new Balance();
            b.setUserId(userId);
            b.setAmount(BigDecimal.ZERO);
            return b;
        });
        log.debug("Applying top-up: userId={} currentAmount={} add={}", userId, balance.getAmount(), amount);
        balance.setAmount(balance.getAmount().add(amount));
        Balance saved = balanceRepository.saveAndFlush(balance);
        log.debug("Top-up saved: userId={} newAmount={} id={}", userId, saved.getAmount(), saved.getId());
    }

    @Transactional(readOnly = true, transactionManager = "balanceTransactionManager")
    public BalanceResponse getBalanceResponseForUserEmail(String email) {
        Optional<Balance> balanceOpt = findBalanceForUserEmail(email);
        log.debug("getBalanceResponseForUserEmail: email={} found={}", email, balanceOpt.isPresent());

        if (balanceOpt.isEmpty()) {
            return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        }

        Balance b = balanceOpt.get();
        OffsetDateTime updated = b.getUpdatedAt() != null ? b.getUpdatedAt().atOffset(ZoneOffset.UTC) : OffsetDateTime.now(ZoneOffset.UTC);
        BigDecimal amount = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amount, currencyCode, updated);
    }

    public boolean applyTopUpForEmail(String email, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        applyTopUp(user.getId(), amount);
        return true;
    }

    @Transactional(transactionManager = "transactionManager")
    public boolean applyTopUpForAuthentication(Authentication auth, BigDecimal amount) {
        if (auth == null) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        User user = userService.findOrCreateFromAuthentication(auth);
        if (user == null || user.getId() == null) return false;

        // update balance within the same JTA transaction
        Balance balance = balanceRepository.findByUserId(user.getId()).orElseGet(() -> {
            Balance b = new Balance();
            b.setUserId(user.getId());
            b.setAmount(BigDecimal.ZERO);
            return b;
        });
        log.debug("Applying top-up (JTA flow): userId={} currentAmount={} add={}", user.getId(), balance.getAmount(), amount);
        balance.setAmount(balance.getAmount().add(amount));
        balanceRepository.saveAndFlush(balance);
        return true;
    }

    @Transactional(readOnly = true, transactionManager = "balanceTransactionManager")
    public BalanceResponse getBalanceResponseForAuthentication(Authentication auth) {
        if (auth == null) return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        User user = userService.findOrCreateFromAuthentication(auth);
        if (user == null) return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        return getBalanceResponseForUserEmail(user.getEmail());
    }
}