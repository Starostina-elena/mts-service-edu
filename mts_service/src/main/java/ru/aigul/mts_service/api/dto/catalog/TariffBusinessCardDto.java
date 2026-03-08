package ru.aigul.mts_service.api.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffBusinessCardDto {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer speedMbps;
    private String slaDescription;
    private List<ServiceDto> services;
}
