package ru.aigul.mts_service.api.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffSatelliteCardDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer channelsTotal;
    private Integer channelsHd;
}
