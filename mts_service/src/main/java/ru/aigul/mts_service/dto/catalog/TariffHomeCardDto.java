package ru.aigul.mts_service.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffHomeCardDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer speedMbps;
    private Integer tvChannels;
    private List<ServiceDto> services;
}
