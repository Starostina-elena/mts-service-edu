package ru.aigul.mts_service.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffLandlineCardDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer localMinutes;
    private Integer intercityMinutes;
    private Integer mobileMinutes;
}
