package ru.aigul.mts_service.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.aigul.mts_service.model.DeviceType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffDeviceCardDto {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private Integer internetGb;
    private DeviceType deviceType;
}
