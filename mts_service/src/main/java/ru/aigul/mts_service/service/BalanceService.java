package ru.aigul.mts_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.exception.UserNotFoundException;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.oracle.model.BalanceOracle;
import ru.aigul.mts_service.oracle.repository.OracleBalanceRepository;
import ru.aigul.mts_service.repository.BalanceRepository;

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
    private final Optional<OracleBalanceRepository> oracleBalanceRepository;
    private final TransactionTemplate transactionTemplate;
    private final java.util.concurrent.ScheduledExecutorService scheduledExecutorService;

    @Value("${payments.base-url}")
    private String paymentsBaseUrl;

    @Value("${app.currency.code}")
    private String currencyCode;

    public Optional<BalanceOracle> findBalanceForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (oracleBalanceRepository.isEmpty()) {
            return Optional.empty();
        }
        return oracleBalanceRepository.get().findByUserId(user.getId());
    }

    @Transactional
    public BalanceResponse applyTopUpForUserEmail(String email, BigDecimal amount) {
        if (oracleBalanceRepository.isEmpty()) {
            throw new IllegalStateException("Oracle balance store is not configured");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        Optional<BalanceOracle> balOpt = oracleBalanceRepository.get().findByUserIdForUpdate(user.getId());
        BalanceOracle bal;
        if (balOpt.isPresent()) {
            bal = balOpt.get();
            bal.setAmount(bal.getAmount().add(amount));
        } else {
            bal = new BalanceOracle();
            bal.setUserId(user.getId());
            bal.setAmount(amount);
        }
        bal = oracleBalanceRepository.get().save(bal);

        OffsetDateTime updated = Optional.ofNullable(bal.getUpdatedAt())
                .map(d -> d.atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
        BigDecimal amt = bal.getAmount() != null ? bal.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amt, currencyCode, updated);
    }

    public Optional<String> createTopUpPayment(String email, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        if (oracleBalanceRepository.isEmpty()) {
            log.warn("Oracle balance repository not configured: createTopUpPayment will not schedule top-up");
            return Optional.empty();
        }

        String paymentId = UUID.randomUUID().toString();
        String base = paymentsBaseUrl;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String paymentUrl = base + paymentId;

        log.debug("PaymentUrl: {}", paymentUrl);

        scheduledExecutorService.schedule(() -> {
            try {
                log.info("Scheduled top-up started for email={} amount={}", email, amount);
                applyTopUpForUserEmail(email, amount);
                log.info("Scheduled top-up finished for email={} amount={}", email, amount);
            } catch (Exception e) {
                log.error("Scheduled top-up failed for email={}, amount={}: {}", email, amount, e.getMessage(), e);
            }
        }, 5, java.util.concurrent.TimeUnit.SECONDS);

        return Optional.of(paymentUrl);
    }

    public BalanceResponse getBalanceResponseForUserEmail(String email) {
        Optional<BalanceOracle> balanceOpt = findBalanceForUserEmail(email);

        if (balanceOpt.isEmpty()) {
            return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        }

        BalanceOracle b = balanceOpt.get();
        OffsetDateTime updated = Optional.ofNullable(b.getUpdatedAt())
                .map(d -> d.atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
        BigDecimal amount = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amount, currencyCode, updated);
    }
}