package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.dto.catalog.*;
import ru.aigul.mts_service.exception.TariffNotFoundException;
import ru.aigul.mts_service.mapper.CityMapper;
import ru.aigul.mts_service.mapper.ServiceMapper;
import ru.aigul.mts_service.mapper.TariffMapper;
import ru.aigul.mts_service.model.*;
import ru.aigul.mts_service.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final TariffRepository tariffRepository;
    private final TariffCityPriceRepository tariffCityPriceRepository;
    private final CityRepository cityRepository;
    private final ServiceRepository serviceRepository;
    private final TariffMapper tariffMapper;
    private final CityMapper cityMapper;
    private final ServiceMapper serviceMapper;


    public CursorPage<TariffHomeCardDto> getHome(Long cityId, BigDecimal priceMax, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findHome(cityId, priceMax, after, Limit.of(limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return CursorPage.of(raw, limit, t -> tariffMapper.toHomeCard(t, prices.get(t.getId())), Tariff::getId);
    }

    public CursorPage<TariffMobileCardDto> getMobile(Integer internetGb, Integer callsMinutes,
                                                      Boolean forFamily, Boolean noSubscriptionFee,
                                                      Long after, int limit) {
        List<Tariff> raw = tariffRepository.findMobile(internetGb, callsMinutes, forFamily, noSubscriptionFee,
                after, Limit.of(limit + 1));
        return CursorPage.of(raw, limit, tariffMapper::toMobileCard, Tariff::getId);
    }

    public CursorPage<TariffDeviceCardDto> getDevices(Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByStatusAndCategoryWithServices(
                TariffStatus.ACTIVE, TariffCategory.DEVICES, after, Limit.of(limit + 1));
        return CursorPage.of(raw, limit, tariffMapper::toDeviceCard, Tariff::getId);
    }

    public CursorPage<TariffLandlineCardDto> getLandline(Long cityId, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByCity(TariffCategory.LANDLINE, cityId, after, Limit.of(limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return CursorPage.of(raw, limit, t -> tariffMapper.toLandlineCard(t, prices.get(t.getId())), Tariff::getId);
    }

    public CursorPage<TariffSatelliteCardDto> getSatelliteTv(Long cityId, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByCity(TariffCategory.SATELLITE_TV, cityId, after, Limit.of(limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return CursorPage.of(raw, limit, t -> tariffMapper.toSatelliteCard(t, prices.get(t.getId())), Tariff::getId);
    }

    public CursorPage<TariffBusinessCardDto> getBusiness(Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByStatusAndCategoryWithServices(
                TariffStatus.ACTIVE, TariffCategory.BUSINESS, after, Limit.of(limit + 1));
        return CursorPage.of(raw, limit, tariffMapper::toBusinessCard, Tariff::getId);
    }

    public TariffDetailDto getTariff(Long tariffId, Long cityId) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new TariffNotFoundException(tariffId));
        Optional<BigDecimal> cityPrice = Optional.empty();
        if (cityId != null) {
            cityPrice = tariffCityPriceRepository.findByTariffIdAndCityId(tariffId, cityId)
                    .map(TariffCityPrice::getPrice);
        }
        return tariffMapper.toDetail(tariff, cityPrice);
    }

    public List<CityDto> getCities() {
        return cityRepository.findAll().stream().map(cityMapper::toDto).toList();
    }

    public List<ServiceDto> getServices() {
        return serviceRepository.findAll().stream().map(serviceMapper::toDto).toList();
    }

    private Map<Long, BigDecimal> cityPriceMap(List<Tariff> tariffs, Long cityId) {
        List<Long> ids = tariffs.stream().map(Tariff::getId).toList();
        return tariffCityPriceRepository.findByTariffIdIn(ids).stream()
                .filter(tcp -> tcp.getCity().getId().equals(cityId))
                .collect(Collectors.toMap(tcp -> tcp.getTariff().getId(), TariffCityPrice::getPrice));
    }

}
