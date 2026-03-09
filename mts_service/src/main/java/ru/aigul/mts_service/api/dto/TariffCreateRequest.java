package ru.aigul.mts_service.api.dto;

import lombok.Data;
import ru.aigul.mts_service.model.DeviceType;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.model.TariffStatus;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TariffCreateRequest {
    private String name;
    private String category;
    private BigDecimal price;
    private String description;

    // home, business
    private Integer speed_mbps;
    private Integer tv_channels;
    private List<String> service_ids;

    // city prices
    private List<CityPriceDto> city_prices;

    // mobile
    private Integer internet_gb;
    private Integer calls_minutes;
    private Integer sms_count;
    private Boolean for_family;
    private Boolean no_subscription_fee;

    // devices
    private DeviceType device_type;

    // landline
    private Integer local_minutes;
    private Integer intercity_minutes;
    private Integer mobile_minutes;

    // satellite
    private Integer channels_total;
    private Integer channels_hd;

    // business
    private String sla_description;

    @Data
    public static class CityPriceDto {
        private String city_id;
        private BigDecimal price;
    }
}

