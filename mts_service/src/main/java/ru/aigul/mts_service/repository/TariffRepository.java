package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.Tariff;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
}
