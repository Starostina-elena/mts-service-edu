package ru.aigul.mts_service.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String role;
    private LocalDateTime createdAt;

    public UserResponse() {}

    public UserResponse(Long id, String email, String name, String role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.createdAt = createdAt;
    }
}
