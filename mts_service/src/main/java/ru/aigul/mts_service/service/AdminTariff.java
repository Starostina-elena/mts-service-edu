package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.dto.TariffCreateRequest;
import ru.aigul.mts_service.exception.TariffAlreadyArchivedException;
import ru.aigul.mts_service.exception.TariffNotFoundException;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCityPrice;
import ru.aigul.mts_service.model.TariffStatus;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.repository.CityRepository;
import ru.aigul.mts_service.repository.ServiceRepository;
import ru.aigul.mts_service.repository.TariffCityPriceRepository;
import ru.aigul.mts_service.repository.TariffRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTariff {
    private final TariffRepository tariffRepository;
    private final CityRepository cityRepository;
    private final ServiceRepository serviceRepository;
    private final TariffCityPriceRepository tariffCityPriceRepository;

    public Tariff saveTariff(Tariff tariff) {
        return tariffRepository.save(tariff);
    }

    public void archiveTariff(Long id) {
        Optional<Tariff> opt = tariffRepository.findById(id);
        if (opt.isEmpty()) throw new TariffNotFoundException(id);
        Tariff t = opt.get();
        if (t.getStatus() == TariffStatus.ARCHIVED) throw new TariffAlreadyArchivedException(id);
        t.setStatus(TariffStatus.ARCHIVED);
        tariffRepository.save(t);
    }

    public Tariff createTariff(TariffCreateRequest req) {
        TariffCategory category = normalizeCategory(req.getCategory());

        Tariff tariff = new Tariff();
        tariff.setName(req.getName());
        tariff.setDescription(req.getDescription());
        tariff.setBasePrice(req.getPrice());
        tariff.setCategory(category);

        if (category == TariffCategory.HOME || category == TariffCategory.BUSINESS) {
            tariff.setSpeedMbps(req.getSpeed_mbps());
            tariff.setTvChannels(req.getTv_channels());
        }

        if (category == TariffCategory.MOBILE) {
            tariff.setInternetGb(req.getInternet_gb());
            tariff.setCallsMinutes(req.getCalls_minutes());
            tariff.setSmsCount(req.getSms_count());
            tariff.setForFamily(req.getFor_family() != null ? req.getFor_family() : false);
            tariff.setNoSubscriptionFee(req.getNo_subscription_fee() != null ? req.getNo_subscription_fee() : false);
        }

        if (category == TariffCategory.DEVICES) {
            tariff.setInternetGb(req.getInternet_gb());
            tariff.setDeviceType(req.getDevice_type());
        }

        if (category == TariffCategory.LANDLINE) {
            tariff.setLocalMinutes(req.getLocal_minutes());
            tariff.setIntercityMinutes(req.getIntercity_minutes());
            tariff.setMobileMinutes(req.getMobile_minutes());
        }

        if (category == TariffCategory.SATELLITE_TV) {
            tariff.setChannelsTotal(req.getChannels_total());
            tariff.setChannelsHd(req.getChannels_hd());
        }

        if (category == TariffCategory.BUSINESS) {
            tariff.setSlaDescription(req.getSla_description());
        }

        if (req.getService_ids() != null && !req.getService_ids().isEmpty()) {
            Set<ru.aigul.mts_service.model.Service> services = resolveServicesByIds(req.getService_ids());
            if (!services.isEmpty()) tariff.setServices(services);
        }

        Tariff saved = saveTariff(tariff);

        saveCityPrices(saved, req.getCity_prices());

        return saved;
    }

    public Set<ru.aigul.mts_service.model.Service> resolveServicesByIds(List<String> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) return new HashSet<>();
        List<Long> sids = serviceIds.stream().filter(s -> s != null && !s.isBlank()).map(s -> {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("service_ids must contain numeric ids only, invalid value: '" + s + "'");
            }
        }).collect(Collectors.toList());

        if (sids.isEmpty()) return new HashSet<>();

        List<ru.aigul.mts_service.model.Service> found = serviceRepository.findAllById(sids);
        if (found.size() != sids.size()) {
            throw new IllegalArgumentException("some services not found");
        }
        return new HashSet<>(found);
    }

    public void saveCityPrices(Tariff savedTariff, List<TariffCreateRequest.CityPriceDto> cityPrices) {
        if (cityPrices == null || cityPrices.isEmpty()) return;
        for (TariffCreateRequest.CityPriceDto cp : cityPrices) {
            if (cp.getCity_id() == null || cp.getPrice() == null) {
                throw new IllegalArgumentException("city_prices entries must have city_id and price");
            }
            Optional<ru.aigul.mts_service.model.City> cityOpt = cityRepository.findByCode(cp.getCity_id());
            if (cityOpt.isEmpty()) {
                throw new IllegalArgumentException("city not found: " + cp.getCity_id());
            }
            TariffCityPrice tcp = new TariffCityPrice();
            tcp.setTariff(savedTariff);
            tcp.setCity(cityOpt.get());
            tcp.setPrice(cp.getPrice());
            tariffCityPriceRepository.save(tcp);
        }
    }

    private TariffCategory normalizeCategory(String raw) {
        if (raw == null) throw new IllegalArgumentException("category is required");
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "home" -> TariffCategory.HOME;
            case "mobile" -> TariffCategory.MOBILE;
            case "devices" -> TariffCategory.DEVICES;
            case "landline" -> TariffCategory.LANDLINE;
            case "satellite_tv", "satellite-tv", "satellite" -> TariffCategory.SATELLITE_TV;
            case "business" -> TariffCategory.BUSINESS;
            default -> throw new IllegalArgumentException("unknown category");
        };
    }

}
