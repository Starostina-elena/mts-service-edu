package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.repository.TariffRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TechnicalFeasibilityService {

    private final TariffRepository tariffRepository;

    public record Result(boolean feasible, String reason) {
    }

    public Result check(String address, String tariffId) {
        if (address == null || address.trim().length() < 10) {
            return new Result(false, "address too short");
        }

        if (tariffId == null || !tariffId.matches("\\d+")) {
            return new Result(false, "invalid tariff_id");
        }

        Long tid;
        try {
            tid = Long.parseLong(tariffId);
        } catch (Exception e) {
            return new Result(false, "invalid tariff_id");
        }

        Optional<Tariff> tOpt = tariffRepository.findById(tid);
        if (tOpt.isEmpty()) {
            return new Result(false, "tariff not found");
        }

        Tariff t = tOpt.get();
        TariffCategory cat = t.getCategory();
        if (cat != TariffCategory.HOME && cat != TariffCategory.LANDLINE && cat != TariffCategory.SATELLITE_TV) {
            return new Result(false, "tariff not applicable for technical check");
        }

        String lower = address.toLowerCase();
        if (lower.contains("неверно") || lower.contains("нет") || lower.contains("unavailable") || lower.contains("no")) {
            return new Result(false, "coverage not available at address");
        }

        return new Result(true, null);
    }
}
