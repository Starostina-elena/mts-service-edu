package ru.aigul.mts_service.api.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffMobileCardDto {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer internetGb;
    private Integer callsMinutes;
    private Integer smsCount;
    private Boolean forFamily;
    private Boolean noSubscriptionFee;
}
