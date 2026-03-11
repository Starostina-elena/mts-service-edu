package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.api.dto.catalog.*;
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


    public CatalogPageDto<TariffHomeCardDto> getHome(Long cityId, BigDecimal priceMax, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findHome(cityId, priceMax, after, PageRequest.of(0, limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return toPage(raw, limit, t -> tariffMapper.toHomeCard(t, prices.get(t.getId())));
    }

    public CatalogPageDto<TariffMobileCardDto> getMobile(Integer internetGb, Integer callsMinutes,
                                                          Boolean forFamily, Boolean noSubscriptionFee,
                                                          Long after, int limit) {
        List<Tariff> raw = tariffRepository.findMobile(internetGb, callsMinutes, forFamily, noSubscriptionFee,
                after, PageRequest.of(0, limit + 1));
        return toPage(raw, limit, tariffMapper::toMobileCard);
    }

    public CatalogPageDto<TariffDeviceCardDto> getDevices(Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByStatusAndCategoryAndIdGreaterThanOrderByIdAsc(
                TariffStatus.ACTIVE, TariffCategory.DEVICES, cursorOrZero(after), PageRequest.of(0, limit + 1));
        return toPage(raw, limit, tariffMapper::toDeviceCard);
    }

    public CatalogPageDto<TariffLandlineCardDto> getLandline(Long cityId, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByCity(TariffCategory.LANDLINE, cityId, after, PageRequest.of(0, limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return toPage(raw, limit, t -> tariffMapper.toLandlineCard(t, prices.get(t.getId())));
    }

    public CatalogPageDto<TariffSatelliteCardDto> getSatelliteTv(Long cityId, Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByCity(TariffCategory.SATELLITE_TV, cityId, after, PageRequest.of(0, limit + 1));
        Map<Long, BigDecimal> prices = cityPriceMap(raw, cityId);
        return toPage(raw, limit, t -> tariffMapper.toSatelliteCard(t, prices.get(t.getId())));
    }

    public CatalogPageDto<TariffBusinessCardDto> getBusiness(Long after, int limit) {
        List<Tariff> raw = tariffRepository.findByStatusAndCategoryAndIdGreaterThanOrderByIdAsc(
                TariffStatus.ACTIVE, TariffCategory.BUSINESS, cursorOrZero(after), PageRequest.of(0, limit + 1));
        return toPage(raw, limit, tariffMapper::toBusinessCard);
    }

    public TariffDetailDto getTariff(Long tariffId) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new TariffNotFoundException(tariffId));
        return tariffMapper.toDetail(tariff, Optional.empty());
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

    private <T> CatalogPageDto<T> toPage(List<Tariff> raw, int limit, java.util.function.Function<Tariff, T> mapper) {
        boolean hasNext = raw.size() > limit;
        List<Tariff> page = hasNext ? raw.subList(0, limit) : raw;
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new CatalogPageDto<>(page.stream().map(mapper).toList(), nextCursor);
    }

    private long cursorOrZero(Long after) {
        return Optional.ofNullable(after).orElse(0L);
    }
}
