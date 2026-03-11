package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.Service;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}

