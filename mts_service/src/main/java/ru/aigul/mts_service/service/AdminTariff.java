package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.exception.TariffAlreadyArchivedException;
import ru.aigul.mts_service.exception.TariffNotFoundException;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffStatus;
import ru.aigul.mts_service.repository.TariffRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminTariff {
    private final TariffRepository tariffRepository;

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

}
