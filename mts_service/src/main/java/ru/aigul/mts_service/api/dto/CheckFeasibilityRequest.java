package ru.aigul.mts_service.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckFeasibilityRequest {
    @NotBlank
    private String address;

    @NotBlank
    private String tariff_id;
}

