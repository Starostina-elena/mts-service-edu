package ru.aigul.mts_service.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.service.DistributedTransactionService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin")
public class DistributedTxController {

    private final DistributedTransactionService txService;

    public DistributedTxController(DistributedTransactionService txService) {
        this.txService = txService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody TransferRequest req) {
        txService.transferBetweenSystems(req.getUserId(), req.getAmount());
        return ResponseEntity.ok("OK");
    }

    public static class TransferRequest {
        private Long userId;
        private BigDecimal amount;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}

