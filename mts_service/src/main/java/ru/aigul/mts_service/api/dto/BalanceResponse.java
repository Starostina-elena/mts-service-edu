package ru.aigul.mts_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime updated_at;
}

