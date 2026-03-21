package ru.aigul.mts_service.dto.application;

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
