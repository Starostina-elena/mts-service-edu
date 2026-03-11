package ru.aigul.mts_service.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private String id;
    private String user_id;
    private String tariff_id;
    private String tariff_name;
    private String address;
    private String status;
    private String reject_reason;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
}
