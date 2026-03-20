package ru.aigul.mts_service.api.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ErrorResponse {
    private final String error;
    private final Instant timestamp;

    public ErrorResponse(String error) {
        this.error = error;
        this.timestamp = Instant.now();
    }
}
