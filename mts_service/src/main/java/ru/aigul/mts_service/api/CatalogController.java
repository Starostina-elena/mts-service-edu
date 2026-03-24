package ru.aigul.mts_service.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import ru.aigul.mts_service.dto.catalog.*;
import ru.aigul.mts_service.service.CatalogService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @Value("${app.pagination.default-limit}")
    private int defaultLimit;

    @GetMapping("/home")
    public CatalogPageDto<TariffHomeCardDto> getHome(
            @RequestParam Long cityId,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getHome(cityId, priceMax, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/mobile")
    public CatalogPageDto<TariffMobileCardDto> getMobile(
            @RequestParam(required = false) Integer internetGb,
            @RequestParam(required = false) Integer callsMinutes,
            @RequestParam(required = false) Boolean forFamily,
            @RequestParam(required = false) Boolean noSubscriptionFee,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getMobile(internetGb, callsMinutes, forFamily, noSubscriptionFee, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/devices")
    public CatalogPageDto<TariffDeviceCardDto> getDevices(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getDevices(after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/landline")
    public CatalogPageDto<TariffLandlineCardDto> getLandline(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getLandline(cityId, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/satellite-tv")
    public CatalogPageDto<TariffSatelliteCardDto> getSatelliteTv(
            @RequestParam Long cityId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getSatelliteTv(cityId, after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/business")
    public CatalogPageDto<TariffBusinessCardDto> getBusiness(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit) {
        return catalogService.getBusiness(after, limit != null ? limit : defaultLimit);
    }

    @GetMapping("/tariffs/{tariffId}")
    public TariffDetailDto getTariff(@PathVariable Long tariffId,
                                     @RequestParam(required = false) Long cityId) {
        return catalogService.getTariff(tariffId, cityId);
    }

    @GetMapping("/cities")
    public List<CityDto> getCities() {
        return catalogService.getCities();
    }

    @GetMapping("/services")
    public List<ServiceDto> getServices() {
        return catalogService.getServices();
    }
}