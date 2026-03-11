package ru.aigul.mts_service.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.aigul.mts_service.model.DeviceType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TariffCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal price;

    private String description;

    // home, business
    private Integer speed_mbps;
    private Integer tv_channels;

    @Valid
    private List<String> service_ids = new ArrayList<>();

    // city prices
    @Valid
    private List<CityPriceDto> city_prices = new ArrayList<>();

    // mobile
    private Integer internet_gb;
    private Integer calls_minutes;
    private Integer sms_count;
    private Boolean for_family = Boolean.FALSE;
    private Boolean no_subscription_fee = Boolean.FALSE;

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
        @NotBlank
        private String city_id;

        @NotNull
        @DecimalMin(value = "0.01")
        private BigDecimal price;
    }
}
