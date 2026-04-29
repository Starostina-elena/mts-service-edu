package ru.aigul.mts_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.aigul.mts_service.dto.BalanceResponse;
import ru.aigul.mts_service.exception.UserNotFoundException;
import ru.aigul.mts_service.model.Balance;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.BalanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final TransactionTemplate transactionTemplate;
    private final java.util.concurrent.ScheduledExecutorService scheduledExecutorService;

    @Value("${payments.base-url}")
    private String paymentsBaseUrl;

    @Value("${app.currency.code}")
    private String currencyCode;

    public Optional<Balance> findBalanceForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        return balanceRepository.findByUserId(user.getId());
    }

    @Transactional
    public BalanceResponse applyTopUpForUserEmail(String email, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User user = userService.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        // Use Postgres balances table. Lock row with PESSIMISTIC_WRITE via repository method.
        Optional<Balance> balOpt = balanceRepository.findByUserIdForUpdate(user.getId());
        Balance bal;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (balOpt.isPresent()) {
            bal = balOpt.get();
            BigDecimal before = bal.getAmount() != null ? bal.getAmount() : BigDecimal.ZERO;
            BigDecimal after = before.add(amount);
            log.info("TopUp: userId={} before={} add={} after={}", user.getId(), before, amount, after);
            bal.setAmount(after);
            bal.setUpdatedAt(now);
        } else {
            bal = new Balance();
            bal.setUser(user);
            bal.setAmount(amount);
            log.info("TopUp: creating new PG balance userId={} amount={}", user.getId(), amount);
            bal.setUpdatedAt(now);
        }
        bal = balanceRepository.save(bal);

        OffsetDateTime updated = bal.getUpdatedAt() != null
                ? bal.getUpdatedAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now(ZoneOffset.UTC);
        BigDecimal amt = bal.getAmount() != null ? bal.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amt, currencyCode, updated);
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

        // Выполняем пополнение синхронно — чтобы клиент сразу видел обновлённый баланс
        try {
            log.info("Starting top-up transaction for email={} amount={}", email, amount);
            transactionTemplate.execute(status -> {
                // inline logic: update PG balances
                User user = userService.findByEmail(email)
                        .orElseThrow(() -> new UserNotFoundException(email));

                Optional<Balance> balOpt = balanceRepository.findByUserIdForUpdate(user.getId());
                Balance bal;
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                if (balOpt.isPresent()) {
                    bal = balOpt.get();
                    BigDecimal before = bal.getAmount() != null ? bal.getAmount() : BigDecimal.ZERO;
                    BigDecimal after = before.add(amount);
                    log.info("TopUp(transaction): userId={} before={} add={} after={}", user.getId(), before, amount, after);
                    bal.setAmount(after);
                    bal.setUpdatedAt(now);
                } else {
                    bal = new Balance();
                    bal.setUser(user);
                    bal.setAmount(amount);
                    log.info("TopUp(transaction): creating new PG balance userId={} amount={}", user.getId(), amount);
                    bal.setUpdatedAt(now);
                }
                balanceRepository.save(bal);
                return null;
            });

            // read back and log
            Optional<User> uo = userService.findByEmail(email);
            if (uo.isPresent()) {
                Long uid = uo.get().getId();
                Optional<Balance> bo = balanceRepository.findByUserId(uid);
                if (bo.isPresent()) {
                    log.info("Post-topup PG balance userId={} amount={}", uid, bo.get().getAmount());
                } else {
                    log.warn("Post-topup: PG balance record not found for userId={}", uid);
                }
            }
        } catch (Exception e) {
            log.error("Top-up failed for email={}, amount={}: {}", email, amount, e.getMessage(), e);
            return Optional.empty();
        }

        return Optional.of(paymentUrl);
    }

    public BalanceResponse getBalanceResponseForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        }
        Long userId = userOpt.get().getId();

        Optional<Balance> balanceOpt = balanceRepository.findByUserId(userId);
        if (balanceOpt.isEmpty()) {
            return new BalanceResponse(BigDecimal.ZERO, currencyCode, OffsetDateTime.now(ZoneOffset.UTC));
        }

        Balance b = balanceOpt.get();
        OffsetDateTime updated = Optional.ofNullable(b.getUpdatedAt())
                .map(d -> d.atOffset(ZoneOffset.UTC))
                .orElse(OffsetDateTime.now(ZoneOffset.UTC));
        BigDecimal amount = b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO;
        return new BalanceResponse(amount, currencyCode, updated);
    }
}