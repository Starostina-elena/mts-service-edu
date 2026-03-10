package ru.aigul.mts_service.api.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationPageDto {
    private List<ApplicationDto> items;
    private Long nextCursor;
}
