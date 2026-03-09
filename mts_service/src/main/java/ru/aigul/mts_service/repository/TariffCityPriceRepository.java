package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.TariffCityPrice;

public interface TariffCityPriceRepository extends JpaRepository<TariffCityPrice, Long> {
}

