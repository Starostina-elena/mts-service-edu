package ru.aigul.mts_service.dto.catalog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.aigul.mts_service.model.DeviceType;
import ru.aigul.mts_service.model.TariffCategory;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TariffDetailDto {
    private Long id;
    private String name;
    private TariffCategory category;
    private BigDecimal price;
    private String description;

    // HOME + BUSINESS
    private Integer speedMbps;

    // HOME
    private Integer tvChannels;

    // HOME + BUSINESS
    private List<ServiceDto> services;

    // MOBILE
    private Integer internetGb;
    private Integer callsMinutes;
    private Integer smsCount;
    private Boolean forFamily;
    private Boolean noSubscriptionFee;

    // DEVICES
    private DeviceType deviceType;

    // LANDLINE
    private Integer localMinutes;
    private Integer intercityMinutes;
    private Integer mobileMinutes;

    // SATELLITE_TV
    private Integer channelsTotal;
    private Integer channelsHd;

    // BUSINESS
    private String slaDescription;
}
