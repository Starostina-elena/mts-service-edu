package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aigul.mts_service.model.Tariff;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
}

