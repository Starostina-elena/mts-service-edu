package ru.aigul.mts_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private HttpStatus status;
    private String message;
    private Map<String, String> errors;
    private OffsetDateTime timestamp;
}
