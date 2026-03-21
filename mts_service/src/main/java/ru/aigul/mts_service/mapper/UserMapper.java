package ru.aigul.mts_service.mapper;

import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.UserResponse;
import ru.aigul.mts_service.model.User;

@Component
public class UserMapper {

    public UserResponse toDto(User u) {
        if (u == null) return null;
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getRole() != null ? u.getRole().name() : null,
                u.getCreatedAt()
        );
    }
}