package ru.aigul.mts_service.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.api.dto.catalog.*;
import ru.aigul.mts_service.model.Tariff;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TariffMapper {

    private final ServiceMapper serviceMapper;

    public TariffHomeCardDto toHomeCard(Tariff t, BigDecimal cityPrice) {
        List<ServiceDto> services = t.getServices().stream()
                .map(serviceMapper::toDto)
                .toList();
        return new TariffHomeCardDto(t.getId(), t.getName(), cityPrice, t.getSpeedMbps(), t.getTvChannels(), services);
    }

    public TariffMobileCardDto toMobileCard(Tariff t) {
        return new TariffMobileCardDto(t.getId(), t.getName(), t.getBasePrice(),
                t.getInternetGb(), t.getCallsMinutes(), t.getSmsCount(),
                t.isForFamily(), t.isNoSubscriptionFee());
    }

    public TariffDeviceCardDto toDeviceCard(Tariff t) {
        return new TariffDeviceCardDto(t.getId(), t.getName(), t.getBasePrice(),
                t.getInternetGb(), t.getDeviceType());
    }

    public TariffLandlineCardDto toLandlineCard(Tariff t, BigDecimal cityPrice) {
        return new TariffLandlineCardDto(t.getId(), t.getName(), cityPrice,
                t.getLocalMinutes(), t.getIntercityMinutes(), t.getMobileMinutes());
    }

    public TariffSatelliteCardDto toSatelliteCard(Tariff t, BigDecimal cityPrice) {
        return new TariffSatelliteCardDto(t.getId(), t.getName(), cityPrice,
                t.getChannelsTotal(), t.getChannelsHd());
    }

    public TariffBusinessCardDto toBusinessCard(Tariff t) {
        List<ServiceDto> services = t.getServices().stream()
                .map(serviceMapper::toDto)
                .toList();
        return new TariffBusinessCardDto(t.getId(), t.getName(), t.getBasePrice(),
                t.getSpeedMbps(), t.getSlaDescription(), services);
    }

    public TariffDetailDto toDetail(Tariff t, BigDecimal cityPrice) {
        List<ServiceDto> services = t.getServices().stream()
                .map(serviceMapper::toDto)
                .toList();
        // cityPrice используется для HOME/LANDLINE/SATELLITE_TV; для остальных — basePrice
        BigDecimal effectivePrice = cityPrice != null ? cityPrice : t.getBasePrice();
        return new TariffDetailDto(
                t.getId(), t.getName(), t.getCategory(), effectivePrice, t.getDescription(),
                t.getSpeedMbps(), t.getTvChannels(), services,
                t.getInternetGb(), t.getCallsMinutes(), t.getSmsCount(),
                t.isForFamily(), t.isNoSubscriptionFee(),
                t.getDeviceType(),
                t.getLocalMinutes(), t.getIntercityMinutes(), t.getMobileMinutes(),
                t.getChannelsTotal(), t.getChannelsHd(),
                t.getSlaDescription()
        );
    }
}
