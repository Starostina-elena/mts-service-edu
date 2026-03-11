package ru.aigul.mts_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CheckFeasibilityResponse {
    private boolean feasible;
    private String reason;
}

