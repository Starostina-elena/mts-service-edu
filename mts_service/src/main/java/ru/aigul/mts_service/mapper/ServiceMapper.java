package ru.aigul.mts_service.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.catalog.ServiceDto;
import ru.aigul.mts_service.model.Service;

@Component
public class ServiceMapper {

    public ServiceDto toDto(Service service) {
        return new ServiceDto(service.getId(), service.getName(), service.getPrice(), service.getDescription());
    }
}
