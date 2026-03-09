package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.model.DeviceType;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.model.TariffStatus;
import ru.aigul.mts_service.repository.TariffRepository;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminTariff {
    private final TariffRepository tariffRepository;

    public Tariff saveTariff(Tariff tariff) {
        return tariffRepository.save(tariff);
    }
}
