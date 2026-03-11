package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.api.dto.PaymentResponse;
import ru.aigul.mts_service.model.Balance;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.BalanceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final UserService userService;
    private final BalanceRepository balanceRepository;

    public Optional<Balance> findBalanceForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        return balanceRepository.findByUserId(user.getId());
    }

    public Optional<PaymentResponse> createTopUpPayment(String email, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        String paymentId = UUID.randomUUID().toString();
        String paymentUrl = "https://payments.example.com/mock-pay/" + paymentId;

        PaymentResponse resp = new PaymentResponse(paymentUrl, paymentId);
        return Optional.of(resp);
    }
}
