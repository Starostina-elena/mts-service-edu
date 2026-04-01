package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.TariffCityPrice;

import java.util.List;
import java.util.Optional;

public interface TariffCityPriceRepository extends JpaRepository<TariffCityPrice, Long> {

    List<TariffCityPrice> findByTariffIdIn(List<Long> tariffIds);

    Optional<TariffCityPrice> findByTariffIdAndCityId(Long tariffId, Long cityId);
}
