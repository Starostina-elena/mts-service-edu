package ru.aigul.mts_service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.service.BalanceService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final BalanceService balanceService;

    @PostMapping(path = "/callback")
    public ResponseEntity<Void> callback(@Valid @RequestBody CallbackRequest req) {
        if (req.getUserId() == null || req.getAmount() == null) {
            return ResponseEntity.badRequest().build();
        }
        balanceService.applyTopUp(req.getUserId(), req.getAmount());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class CallbackRequest {
        @NotNull
        private Long userId;
        @NotNull
        private BigDecimal amount;
    }
}

