package ru.aigul.mts_service.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.catalog.CityDto;
import ru.aigul.mts_service.model.City;

@Component
public class CityMapper {

    public CityDto toDto(City city) {
        return new CityDto(city.getId(), city.getCode(), city.getName(), city.getRegion());
    }
}
