package ru.aigul.mts_service.api.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceCreateRequest {
    private String name;
    private BigDecimal price;
    private String description;
}

