package ru.aigul.mts_service.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
    private String created_at;
    private String updated_at;
}

