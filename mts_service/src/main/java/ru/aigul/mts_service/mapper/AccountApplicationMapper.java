package ru.aigul.mts_service.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.ApplicationResponse;
import ru.aigul.mts_service.model.Application;

@Component
public class AccountApplicationMapper {

    public ApplicationResponse toDto(Application a) {
        if (a == null) {
            return null;
        }
        ApplicationResponse dto = new ApplicationResponse();
        dto.setId(a.getId() != null ? String.valueOf(a.getId()) : null);
        dto.setUser_id(a.getUser() != null && a.getUser().getId() != null ? String.valueOf(a.getUser().getId()) : null);
        dto.setTariff_id(a.getTariff() != null && a.getTariff().getId() != null ? String.valueOf(a.getTariff().getId()) : null);
        dto.setTariff_name(a.getTariff() != null ? a.getTariff().getName() : null);
        dto.setAddress(a.getAddress());
        dto.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        dto.setReject_reason(a.getRejectReason());
        dto.setCreated_at(a.getCreatedAt());
        dto.setUpdated_at(a.getUpdatedAt());
        return dto;
    }
}