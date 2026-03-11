package ru.aigul.mts_service.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopUpRequest {
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}

