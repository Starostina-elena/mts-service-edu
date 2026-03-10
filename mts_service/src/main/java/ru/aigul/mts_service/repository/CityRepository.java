package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.City;

public interface CityRepository extends JpaRepository<City, Long> {
}
