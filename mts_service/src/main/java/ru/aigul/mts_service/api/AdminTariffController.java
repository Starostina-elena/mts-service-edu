package ru.aigul.mts_service.api;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aigul.mts_service.api.dto.TariffCreateRequest;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCityPrice;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.repository.CityRepository;
import ru.aigul.mts_service.repository.ServiceRepository;
import ru.aigul.mts_service.repository.TariffCityPriceRepository;
import ru.aigul.mts_service.service.AdminTariff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTariffController {

    private final AdminTariff adminTariffService;
    private final CityRepository cityRepository;
    private final ServiceRepository serviceRepository;
    private final TariffCityPriceRepository tariffCityPriceRepository;

    @PostMapping("/tariffs")
    public ResponseEntity<?> createTariff(@RequestBody TariffCreateRequest req) {

        if (req.getName() == null || req.getName().isBlank() || req.getCategory() == null || req.getPrice() == null) {
            return ResponseEntity.unprocessableEntity().body("name, category and price are required");
        }

        TariffCategory category;
        try {
            category = normalizeCategory(req.getCategory());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.unprocessableEntity().body("unknown category");
        }

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

        Set<ru.aigul.mts_service.model.Service> services = null;
        if (req.getService_ids() != null && !req.getService_ids().isEmpty()) {
            List<Long> sids = new ArrayList<>();
            for (String sid : req.getService_ids()) {
                if (sid == null || sid.isBlank()) {
                    continue;
                }
                try {
                    sids.add(Long.parseLong(sid));
                } catch (NumberFormatException e) {
                    return ResponseEntity.unprocessableEntity().body("service_ids must contain numeric ids only, invalid value: '" + sid + "'");
                }
            }
            if (!sids.isEmpty()) {
                List<ru.aigul.mts_service.model.Service> found = serviceRepository.findAllById(sids);
                if (found.size() != sids.size()) {
                    return ResponseEntity.unprocessableEntity().body("some services not found");
                }
                services = found.stream().collect(Collectors.toSet());
                tariff.setServices(services);
            }
        }

        Tariff saved = adminTariffService.saveTariff(tariff);

        if (req.getCity_prices() != null && !req.getCity_prices().isEmpty()) {
            for (TariffCreateRequest.CityPriceDto cp : req.getCity_prices()) {
                if (cp.getCity_id() == null || cp.getPrice() == null) {
                    return ResponseEntity.unprocessableEntity().body("city_prices entries must have city_id and price");
                }
                Optional<ru.aigul.mts_service.model.City> cityOpt = cityRepository.findByCode(cp.getCity_id());
                if (cityOpt.isEmpty()) {
                    return ResponseEntity.unprocessableEntity().body("city not found: " + cp.getCity_id());
                }
                TariffCityPrice tcp = new TariffCityPrice();
                tcp.setTariff(saved);
                tcp.setCity(cityOpt.get());
                tcp.setPrice(cp.getPrice());
                tariffCityPriceRepository.save(tcp);
            }
        }

        return ResponseEntity.status(201).body(java.util.Map.of("id", saved.getId()));
    }

    private TariffCategory normalizeCategory(String raw) {
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "home" -> TariffCategory.HOME;
            case "mobile" -> TariffCategory.MOBILE;
            case "devices" -> TariffCategory.DEVICES;
            case "landline" -> TariffCategory.LANDLINE;
            case "satellite_tv", "satellite-tv", "satellite" -> TariffCategory.SATELLITE_TV;
            case "business" -> TariffCategory.BUSINESS;
            default -> throw new IllegalArgumentException("unknown");
        };
    }
}
