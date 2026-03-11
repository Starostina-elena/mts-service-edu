package ru.aigul.mts_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {
    private String payment_url;
    private String payment_id;
}

