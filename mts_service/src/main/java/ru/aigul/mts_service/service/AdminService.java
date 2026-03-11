package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.repository.ServiceRepository;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class AdminService {
    private final ServiceRepository serviceRepository;

    public long createService(String name, BigDecimal price, String description) {
        ru.aigul.mts_service.model.Service s = new ru.aigul.mts_service.model.Service();
        s.setName(name);
        s.setPrice(price);
        s.setDescription(description);
        ru.aigul.mts_service.model.Service saved = serviceRepository.save(s);
        return saved.getId() != null ? saved.getId() : -1L;
    }
}

